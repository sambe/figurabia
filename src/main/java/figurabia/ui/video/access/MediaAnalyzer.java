/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on May 20, 2012
 */
package figurabia.ui.video.access;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IStreamCoder.Direction;
import com.xuggle.xuggler.IVideoPicture;

public class MediaAnalyzer {

    private static final long END_OF_MEDIA = -541478725L;

    public static MediaInfo analyze(File file) throws IOException {
        IContainer container = null;
        IStream[] streams = null;
        IStreamCoder[] coders = null;
        IStream audioStream = null;
        IStream videoStream = null;
        IStreamCoder audioCoder = null;
        IStreamCoder videoCoder = null;
        int videoStreamIndex = -1;
        int audioStreamIndex = -1;

        try {
            // open file
            container = IContainer.make();
            if (container.open(new RandomAccessFile(file, "r"), IContainer.Type.READ, null) < 0) {
                throw new RuntimeException("error opening file");
            }
            int numStreams = container.getNumStreams();
            streams = new IStream[numStreams];
            coders = new IStreamCoder[numStreams];
            for (int i = 0; i < numStreams; i++) {
                IStream stream = container.getStream(i);
                streams[i] = stream;
                IStreamCoder coder = stream.getStreamCoder();
                coders[i] = coder;
                if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                    audioStream = stream;
                    audioCoder = coder;
                    audioStreamIndex = i;
                } else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                    videoStream = stream;
                    videoCoder = coder;
                    videoStreamIndex = i;
                } else {
                    System.err.println("Unknown codec of type " + coder.getCodecType());
                }
            }

            if (audioCoder.open(null, null) < 0)
                throw new RuntimeException("error opening audio stream coder");
            if (videoCoder.open(null, null) < 0)
                throw new RuntimeException("error opening video stream coder");

            IPacket packet = IPacket.make();
            double frameRate = videoCoder.getFrameRate().getDouble();
            long duration = container.getDuration();
            long lastTimestamp = -1;

            // find key frames
            List<Long> keyFrameTimestamps = new ArrayList<Long>();
            List<Double> calculatedFrameRates = new ArrayList<Double>();
            int videoPacketCount = 0;
            while (true) {
                int errorNum;
                if ((errorNum = container.readNextPacket(packet)) < 0) {
                    if (errorNum == END_OF_MEDIA) {
                        break;
                    } else {
                        IError error = IError.make(errorNum);
                        throw new RuntimeException("error reading packet: " + error.getDescription());
                    }
                }

                lastTimestamp = (long) (packet.getTimeStamp() * packet.getTimeBase().getValue() * 1000000L);

                //int size = packet.getSize();
                if (packet.getStreamIndex() == videoStreamIndex) {
                    if (packet.isKey()) {
                        keyFrameTimestamps.add(lastTimestamp);
                        if (lastTimestamp > 0) {
                            double calculatedFrameRate = videoPacketCount * 1000000.0 / (double) lastTimestamp;
                            calculatedFrameRates.add(calculatedFrameRate);
                        }
                    }
                    videoPacketCount++;
                }
            }

            // check calculated frame rates
            double selectedFrameRate = calculatedFrameRates.get(calculatedFrameRates.size() - 1);
            double averageFrameRate = 0;
            for (Double rate : calculatedFrameRates) {
                averageFrameRate += rate;
            }
            averageFrameRate /= calculatedFrameRates.size();

            // find time base corrections for video and audio (timebase of picture was wrong in one case)
            double videoPacketTimeBase = Double.NaN;
            double audioPacketTimeBase = Double.NaN;
            double pictureTimeBase = Double.NaN;
            double samplesTimeBase = Double.NaN;
            int audioFrameSize = -1;
            if (keyFrameTimestamps.size() >= 2) {
                IStreamCoder audioDecoder = null;
                IStreamCoder videoDecoder = null;
                try {
                    audioDecoder = IStreamCoder.make(Direction.DECODING, audioCoder);
                    videoDecoder = IStreamCoder.make(Direction.DECODING, videoCoder);
                    if (audioDecoder.open(null, null) < 0)
                        throw new RuntimeException("error opening audio stream decoder");
                    if (videoDecoder.open(null, null) < 0)
                        throw new RuntimeException("error opening video stream decoder");

                    long seekMicros = keyFrameTimestamps.get(1);
                    if (container.seekKeyFrame(-1, 0, seekMicros, seekMicros, 0) < 0)
                        throw new RuntimeException("error seeking to position " + (seekMicros / 1000.0) + "ms");

                    boolean audioComplete = false;
                    boolean videoComplete = false;
                    IAudioSamples audioSamples = IAudioSamples.make(4096, audioCoder.getChannels());
                    IVideoPicture videoPicture = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(),
                            videoCoder.getHeight());
                    boolean firstAudioPacket = true;
                    boolean firstVideoPacket = true;
                    double audioPacketTime = Double.NaN;
                    double videoPacketTime = Double.NaN;

                    while (!audioComplete || !videoComplete) {
                        int error;
                        if ((error = container.readNextPacket(packet)) < 0) {
                            if (error == END_OF_MEDIA)
                                break;
                            throw new RuntimeException("error reading packet: " + IError.make(error).getDescription());
                        }

                        if (!audioComplete && packet.getStreamIndex() == audioStreamIndex) {
                            if (firstAudioPacket) {
                                audioPacketTimeBase = packet.getTimeBase().getValue();
                                audioPacketTime = packet.getTimeStamp() * audioPacketTimeBase;
                                firstAudioPacket = false;
                            }
                            int offset = 0;
                            while (offset < packet.getSize()) {
                                int bytesDecoded = audioDecoder.decodeAudio(audioSamples, packet, offset);
                                if (bytesDecoded < 0)
                                    throw new RuntimeException("error decoding audio");
                                offset += bytesDecoded;

                                if (audioSamples.isComplete()) {
                                    samplesTimeBase = audioPacketTime / audioSamples.getTimeStamp();
                                    audioFrameSize = audioSamples.getSize();
                                    audioComplete = true;
                                    break;
                                }
                            }
                        } else if (!videoComplete && packet.getStreamIndex() == videoStreamIndex) {
                            if (firstVideoPacket) {
                                videoPacketTimeBase = packet.getTimeBase().getValue();
                                videoPacketTime = packet.getTimeStamp() * videoPacketTimeBase;
                                firstVideoPacket = false;
                            }
                            int offset = 0;
                            while (offset < packet.getSize()) {
                                int bytesDecoded = videoDecoder.decodeVideo(videoPicture, packet, offset);
                                if (bytesDecoded < 0)
                                    throw new RuntimeException("error decoding video");
                                offset += bytesDecoded;

                                if (videoPicture.isComplete()) {
                                    pictureTimeBase = videoPacketTime / videoPicture.getTimeStamp();
                                    videoComplete = true;
                                    break;
                                }
                            }
                        } else {
                            // ignore
                        }
                    }
                    if (!videoComplete)
                        throw new IllegalStateException("no complete video frame was found for video " + file);
                    // commented out, because there are videos without audio
                    //if (!audioComplete)
                    //    throw new IllegalStateException("no complete audio frame was found for video " + file);
                } finally {
                    if (audioDecoder != null && audioDecoder.isOpen())
                        audioDecoder.close();
                    if (videoDecoder != null && videoDecoder.isOpen())
                        videoDecoder.close();
                }
            } else {
                // case less than 2 key frames (not very probable)
                throw new RuntimeException("less than 2 key frames in video");
            }

            pictureTimeBase = 1.0 / (Math.round(1.0 / pictureTimeBase * 1000.0) / 1000.0);
            samplesTimeBase = 1.0 / (Math.round(1.0 / samplesTimeBase * 1000.0) / 1000.0);

            return new MediaInfo(keyFrameTimestamps, videoPacketTimeBase, audioPacketTimeBase, pictureTimeBase,
                    samplesTimeBase, audioFrameSize, averageFrameRate);

        } finally {
            if (audioCoder != null && audioCoder.isOpen())
                audioCoder.close();
            if (videoCoder != null && videoCoder.isOpen())
                videoCoder.close();
            if (container != null && container.isOpened()) {
                container.close();
            }

        }
    }
}
