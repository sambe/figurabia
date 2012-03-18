/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 23.12.2011
 */
package figurabia.ui.video.access;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.IVideoResampler;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

public class XugglerMediaInputStream {

    private final File file;

    private final IContainer container;
    private IStream audioStream;
    private IStream videoStream;
    private final IStreamCoder audioCoder;
    private final IStreamCoder videoCoder;
    private final IVideoResampler videoResampler;
    private final IVideoPicture resamplingTempPic;
    private final IConverter javaImageConverter;

    private final IPacket packet;
    private final IAudioSamples samples;

    private final double audioTimeBase;
    private final double videoTimeBase;
    private final AudioFormat audioFormat;
    private final VideoFormat videoFormat;
    private final long audioFramesSampleNum;
    private final double frameTime;
    private final double audioPacketTimeStep;

    private long intendedPosition = -1;
    private long targetAudioPacket = -1;
    private int targetAudioBytePos = -1;
    private boolean packetReadPartially = false;
    private int samplesBytePos = 0;

    private List<IVideoPicture> createdVideoPictures = new ArrayList<IVideoPicture>();

    public XugglerMediaInputStream(File file) throws IOException {
        this.file = file;
        // open the media file
        container = IContainer.make();
        int resultCode = container.open(new RandomAccessFile(file, "r"), IContainer.Type.READ, null);
        if (resultCode < 0)
            throw new BadVideoException("error opening container (code = " + resultCode);

        // retrieve some information about the media file
        int numStreams = container.getNumStreams();
        long duration = container.getDuration();

        System.out.println("number of streams in container: " + numStreams);
        System.out.println("duration: " + duration);

        for (int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            System.out.println("Stream " + i + " is of type " + coder.getCodecType());
            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                System.out.println("audio sample rate: " + coder.getSampleRate());
                System.out.println("channels: " + coder.getChannels());
                System.out.println("format: " + coder.getCodec().getLongName());
                audioStream = stream;
            } else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                System.out.println("width: " + coder.getWidth());
                System.out.println("height: " + coder.getHeight());
                System.out.println("format: " + coder.getCodec().getLongName());
                videoStream = stream;
            } else {
                // ignore other streams
            }
        }

        if (audioStream == null)
            throw new BadVideoException("no audio stream found");
        if (videoStream == null)
            throw new BadVideoException("no video stream found");

        audioTimeBase = audioStream.getTimeBase().getValue();
        videoTimeBase = videoStream.getTimeBase().getValue();

        audioCoder = audioStream.getStreamCoder();
        videoCoder = videoStream.getStreamCoder();

        audioFormat = createJavaSoundFormat(audioCoder);
        videoFormat = createVideoFormat(videoCoder);
        double exactAudioFramesSampleNum = audioCoder.getSampleRate() / videoFormat.getFrameRate();
        audioFramesSampleNum = (long) Math.ceil(exactAudioFramesSampleNum);
        frameTime = 1000.0 / videoFormat.getFrameRate();

        int audioFrameSize = audioCoder.getAudioFrameSize();
        int audioSampleRate = audioCoder.getSampleRate();
        audioPacketTimeStep = (double) audioFrameSize / (double) audioSampleRate;

        System.out.println("audio samples per video frame: " + audioFramesSampleNum);

        // initialize decoders
        int audioStreamOpenResultCode = audioCoder.open();
        if (audioStreamOpenResultCode < 0)
            throw new IllegalStateException("Could not open audio stream (error " + audioStreamOpenResultCode + ")");

        int videoStreamOpenResultCode = videoCoder.open();
        if (videoStreamOpenResultCode < 0)
            throw new IllegalStateException("Could not open video stream (error " + videoStreamOpenResultCode + ")");

        // initialize resampler
        if (videoCoder.getPixelType() != IPixelFormat.Type.BGR24) {
            // if the video stream is not in BGR24, we'll have to convert it
            videoResampler = IVideoResampler.make(videoCoder.getWidth(), videoCoder.getHeight(),
                    IPixelFormat.Type.BGR24, videoCoder.getWidth(), videoCoder.getHeight(), videoCoder.getPixelType());
            if (videoResampler == null)
                throw new BadVideoException("Could not create video sampler. Probably unsupported color space");
            resamplingTempPic = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(),
                    videoCoder.getHeight());
        } else {
            videoResampler = null;
            resamplingTempPic = null;
        }

        // initialize java image converter
        javaImageConverter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24,
                IPixelFormat.Type.BGR24,
                videoCoder.getWidth(), videoCoder.getHeight());

        // initialize buffers
        packet = IPacket.make();
        samples = IAudioSamples.make(audioCoder.getAudioFrameSize(), audioCoder.getChannels());
    }

    private static AudioFormat createJavaSoundFormat(IStreamCoder audioCoder) {
        float sampleRate = (float) audioCoder.getSampleRate();
        int sampleSize = (int) IAudioSamples.findSampleBitDepth(audioCoder.getSampleFormat());
        int channels = audioCoder.getChannels();
        boolean signed = true; /* xuggler defaults to signed 16 bit samples */
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
    }

    private static VideoFormat createVideoFormat(IStreamCoder videoCoder) {
        String encoding = videoCoder.getCodec().getName();
        Dimension size = new Dimension(videoCoder.getWidth(), videoCoder.getHeight());
        double frameRate = videoCoder.getFrameRate().getValue();
        return new VideoFormat(encoding, size, frameRate);
    }

    public void readFrame(MediaFrame mf) {
        long DEBUG_startMillis = System.currentTimeMillis();
        int skippedVideoFrames = 0;
        int skippedAudioFrames = 0;

        IVideoPicture picture;
        if (videoResampler == null)
            picture = mf.video.videoPicture;
        else
            picture = resamplingTempPic;

        picture.setComplete(false, videoCoder.getPixelType(), videoCoder.getWidth(),
                videoCoder.getHeight(), packet.getPts());

        // loop through the packets
        boolean audioComplete = false;
        boolean videoComplete = false;
        int audioBytePos = 0;
        while ((!videoComplete || !audioComplete) && (packetReadPartially || container.readNextPacket(packet) >= 0)) {
            long packetTimestamp = (long) (packet.getTimeStamp() * packet.getTimeBase().getValue() * 1000.0);
            if (packet.getStreamIndex() == audioStream.getIndex()) {
                if (audioComplete)
                    throw new IllegalStateException("reading an audio packet, even though already read enough");

                int offset = 0;

                while (offset < packet.getSize()) {
                    // only decode packet if previous decoded data has been copied fully
                    if (samplesBytePos == 0) {
                        int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
                        if (bytesDecoded < 0) {
                            throw new IllegalStateException("could not decode audio. Error code " + bytesDecoded);
                        }
                        offset += bytesDecoded;
                    }

                    if (samples.isComplete()) {
                        // first skip audio samples that are before the "intendedPosition" (position that was set with setPosition)
                        if (intendedPosition != -1 && packetTimestamp < targetAudioPacket) { //packetTimestamp + (long) (1.5 * frameTime) < intendedPosition)
                            // reset buffer, so it can start filling again (because we're skipping until intendedPosition)
                            System.out.println("Skipping " + samples.getNumSamples() + " audio samples. ("
                                    + packetTimestamp + ")");
                            //samples.setComplete(false, audioFramesSampleNum, audioCoder.getSampleRate(),
                            //        audioCoder.getChannels(), audioCoder.getSampleFormat(), packet.getPts());
                            skippedAudioFrames++;
                            continue;
                        }
                        if (intendedPosition != -1) {
                            intendedPosition = -1; // to reset (already skipped frames as necessary) 
                            audioBytePos = targetAudioBytePos;
                        }
                        System.out.println("Got " + samples.getNumSamples() + " audio samples! (" + packetTimestamp
                                + ")");
                        packetReadPartially = offset < packet.getSize();
                        //// getByteArray copies the bytes
                        //byte[] audioBytes = samples.getData().getByteArray(0, samples.getSize());
                        byte[] dest = mf.audio.audioData;
                        int bytesToCopy = Math.min(samples.getSize() - samplesBytePos, dest.length - audioBytePos);
                        samples.get(samplesBytePos, dest, audioBytePos, bytesToCopy);
                        samplesBytePos += bytesToCopy;
                        audioBytePos += bytesToCopy;
                        // if "samples" could not be fully copied because the current frame is already filled up
                        if (audioBytePos == dest.length) {
                            audioBytePos = 0; // because it is full now
                            audioComplete = true;
                            break;
                        } else {
                            if (samplesBytePos != samples.getSize())
                                throw new IllegalStateException(
                                        "internal inconsistency: target array not full, but still uncopied bytes available");
                            samplesBytePos = 0; // because it has been fully copied over
                        }
                    }
                }
            } else if (packet.getStreamIndex() == videoStream.getIndex()) {
                if (videoComplete)
                    throw new IllegalStateException("reading a video packet, even though already read enough");

                int offset = 0;

                while (offset < packet.getSize()) {
                    int bytesDecoded = videoCoder.decodeVideo(picture, packet, offset);
                    if (bytesDecoded < 0) {
                        throw new RuntimeException("could not decode video. Error code " + bytesDecoded);
                    }
                    offset += bytesDecoded;

                    /*
                     * Some decoders will consume data in a packet, but will not be able to construct
                     * a full video picture yet.  Therefore you should always check if you
                     * got a complete picture from the decoder
                     */
                    if (picture.isComplete()) {

                        // first skip video frames that are before the "intendedPosition" (position that was set with setPosition)
                        if (intendedPosition != -1 && packetTimestamp < intendedPosition) {
                            // reset buffer (because we're skipping frames until intendedPosition)
                            System.out.println("Skipping a video picture. (" + packetTimestamp + ")");
                            picture.setComplete(false, videoCoder.getPixelType(), videoCoder.getWidth(),
                                    videoCoder.getHeight(), packet.getPts());
                            skippedVideoFrames++;
                            continue;
                        }

                        packetReadPartially = offset < packet.getSize();
                        IVideoPicture newPic;
                        /*
                         * If the resampler is not null, that means we didn't get the
                         * video in BGR24 format and
                         * need to convert it into BGR24 format.
                         */
                        if (videoResampler != null) {
                            // we must resample
                            newPic = mf.video.videoPicture;
                            if (videoResampler.resample(newPic, picture) < 0)
                                throw new RuntimeException("could not resample video from: "
                                        + file);
                        } else {
                            newPic = picture;
                        }

                        // TODO this security check should not be necessary, remove later
                        //if (newPic.getPixelType() != IPixelFormat.Type.BGR24)
                        //    throw new RuntimeException("could not decode video" +
                        //            " as BGR 24 bit data in: " + file);

                        mf.video.bufferedImage = javaImageConverter.toImage(newPic);
                        newPic.setPts(packet.getPts());

                        System.out.println("Received a video picture (" + packetTimestamp + ")");
                        videoComplete = true;
                        break;
                    }
                }
            }
        }

        if (!audioComplete) {
            System.err.println("Audio not complete");
        }
        if (!videoComplete) {
            System.err.println("Video not complete");
        }
        if (skippedAudioFrames != 0 || skippedVideoFrames != 0) {
            System.out.println("TRACE: skipped video frames: " + skippedVideoFrames + "; skipped audio frames: "
                    + skippedAudioFrames);
        }
        long DEBUG_endMillis = System.currentTimeMillis();
        System.err.println("TRACE: read media frame in " + (DEBUG_endMillis - DEBUG_startMillis) + "ms");
    }

    public long setPosition(long millis) {
        //TODO possibly implement alternative seeking method that preserves audio packet in front of key video packet (1. search backwards to video keyframe, 2. search backwards to audio keyframe)
        //     maybe check how seeking is done in Xuggler and maybe propose to mailing list (after checking for discussions of course)
        long micros = (millis - (long) frameTime) * 1000;
        if (micros < 0)
            micros = 0;

        // seek in microseconds (try to find the last position before or at the specified position)
        long statusCode = container.seekKeyFrame(-1, micros - 5000000, micros, micros, 0);
        if (statusCode < 0)
            throw new IllegalStateException("Seek to position " + millis + "ms failed with code " + statusCode
                    + " in file " + file);
        packetReadPartially = false;
        samplesBytePos = 0;
        long actualMillis = (long) (videoStream.getCurrentDts() * videoTimeBase * 1000.0);
        intendedPosition = millis;
        int audioFrameSize = audioCoder.getAudioFrameSize();
        long targetSample = (long) Math.floor((double) millis / 1000.0 / audioPacketTimeStep * audioFrameSize);
        targetAudioPacket = Math.round(Math.floor(targetSample / audioFrameSize) * audioPacketTimeStep * 1000.0);
        targetAudioBytePos = (int) ((targetSample % audioFrameSize) * audioFormat.getSampleSizeInBits() / 8);
        return millis;
    }

    public long getPosition() {
        // TODO maybe time of audio stream has to be regarded as well
        return (long) (videoStream.getCurrentDts() * videoTimeBase * 1000.0);
    }

    public long getDuration() {
        return container.getDuration();
    }

    public VideoFormat getVideoFormat() {
        return videoFormat;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public MediaFrame createFrame() {
        AudioBuffer audio = new AudioBuffer();
        audio.audioData = new byte[(int) audioFramesSampleNum * audioFormat.getSampleSizeInBits() / 8
                * audioFormat.getChannels()];
        VideoBuffer video = new VideoBuffer();
        video.videoPicture = IVideoPicture.make(IPixelFormat.Type.BGR24, videoCoder.getWidth(),
                videoCoder.getHeight());
        createdVideoPictures.add(video.videoPicture.copyReference());
        return new MediaFrame(audio, video);
    }

    public void close() {
        audioCoder.close();
        videoCoder.close();
        container.close();
        for (IVideoPicture p : createdVideoPictures)
            p.delete();
    }

    public static void main(String[] args) {

        IContainer container = IContainer.make();

        int resultCode = container.open(args[0], IContainer.Type.READ, null);
        if (resultCode < 0)
            throw new IllegalStateException("error opening file (code = " + resultCode);

        // retrieve some information about the media file
        int numStreams = container.getNumStreams();
        long duration = container.getDuration();

        System.out.println("number of streams in container: " + numStreams);
        System.out.println("duration: " + duration);

        IStream audioStream = null;
        IStream videoStream = null;

        for (int i = 0; i < numStreams; i++) {
            IStream stream = container.getStream(i);
            IStreamCoder coder = stream.getStreamCoder();

            System.out.println("Stream " + i + " is of type " + coder.getCodecType());
            if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
                System.out.println("audio sample rate: " + coder.getSampleRate());
                System.out.println("channels: " + coder.getChannels());
                System.out.println("format: " + coder.getCodec().getLongName());
                audioStream = stream;
            } else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                System.out.println("width: " + coder.getWidth());
                System.out.println("height: " + coder.getHeight());
                System.out.println("format: " + coder.getCodec().getLongName());
                System.out.println("frame rate: " + coder.getFrameRate());
                videoStream = stream;
            } else {

            }
        }

        if (audioStream == null)
            throw new IllegalStateException("no audio stream found");
        if (videoStream == null)
            throw new IllegalStateException("no video stream found");

        // retrieve audio & video
        IStreamCoder audioCoder = audioStream.getStreamCoder();
        IStreamCoder videoCoder = videoStream.getStreamCoder();

        int audioStreamOpenResultCode = audioCoder.open();
        if (audioStreamOpenResultCode < 0)
            throw new IllegalStateException("Could not open audio stream (error " + audioStreamOpenResultCode + ")");

        IPacket packet = IPacket.make();
        int videoPacketCount = 0;
        int audioPacketCount = 0;
        double videoTimeBase = videoCoder.getTimeBase().getDouble() * 1000.0;
        double audioTimeBase = audioCoder.getTimeBase().getDouble() * 1000.0;
        double videoPacketDiff = 1.0 / videoCoder.getFrameRate().getDouble();
        double audioSampleRate = audioCoder.getSampleRate();
        double audioPacketDiff = audioCoder.getAudioFrameSize() / audioSampleRate;
        while (container.readNextPacket(packet) >= 0) {
            if (packet.getStreamIndex() == audioStream.getIndex()) {
                IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());
                int offset = 0;

                while (offset < packet.getSize()) {
                    int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
                    if (bytesDecoded < 0) {
                        throw new IllegalStateException("could not decode audio. Error code " + bytesDecoded);
                    }
                    offset += bytesDecoded;

                    if (samples.isComplete()) {
                        double realTime = audioPacketCount++ * audioPacketDiff;
                        double fileTime = samples.getPts() * samples.getTimeBase().getDouble(); // * audioTimeBase / 1000.0;
                        System.out.println(String.format("%9.6f %9.6f %9.6f", realTime, fileTime, realTime - fileTime)
                                + ": Got " + samples.getNumSamples()
                                + " audio samples!"
                                + (packet.isKeyPacket() ? " (key)" : ""));
                        // getByteArray copies the bytes
                        byte[] audioBytes = samples.getData().getByteArray(0, samples.getSize());
                    }
                }
            } else if (packet.getStreamIndex() == videoStream.getIndex()) {
                double realTime = videoPacketCount++ * videoPacketDiff;
                double videoDts = packet.getPts();
                double fileTime = videoDts * packet.getTimeBase().getDouble(); //* videoTimeBase / 1000.0;
                System.out.println(String.format("%9.6f %9.6f %9.6f", realTime, fileTime, realTime - fileTime)
                        + ": Got video frame"
                        + (packet.isKeyPacket() ? " (key)" : ""));
            }
        }
    }
}
