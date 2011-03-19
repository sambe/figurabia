/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 17.10.2010
 */
package figurabia.ui.video.access;

import java.io.File;
import java.io.IOException;

import javax.media.BadHeaderException;
import javax.media.Buffer;
import javax.media.Format;
import javax.media.IncompatibleSourceException;
import javax.media.MediaLocator;
import javax.media.Time;
import javax.media.Track;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;

import com.omnividea.media.parser.video.Parser;
import com.omnividea.media.protocol.file.DataSource;

public class MediaInputStream {

    private File file;
    private Parser parser;
    private com.omnividea.media.codec.video.NativeDecoder videoDecoder;
    private com.omnividea.media.codec.audio.NativeDecoder audioDecoder;
    private Track videoTrack;
    private Track audioTrack;
    private VideoFormat videoFormat;
    private AudioFormat audioFormat;
    private DataSource fileDataSource;

    private Buffer videoFrame;
    private Buffer audioFrame;
    private Buffer decodedAudioFrame;
    private int decodedAudioFrameAvailable = 0;

    private int audioBufferBytes;
    private int videoBufferInts;

    public MediaInputStream(File file) throws IOException {
        this.file = file;
        MediaLocator mediaLocator = new MediaLocator("file:" + file.getAbsolutePath());
        fileDataSource = new DataSource();
        fileDataSource.setLocator(mediaLocator);
        fileDataSource.connect();

        parser = new Parser();

        videoDecoder = new com.omnividea.media.codec.video.NativeDecoder();
        audioDecoder = new com.omnividea.media.codec.audio.NativeDecoder();

        // init parser
        try {
            parser.setSource(fileDataSource);
        } catch (IncompatibleSourceException e) {
            throw new IllegalStateException("should never happen (internal error)", e);
        }
        parser.open();
        parser.start();
        Track[] tracks;
        try {
            tracks = parser.getTracks();
        } catch (BadHeaderException e) {
            throw new BadVideoException("bad header in: " + file.getAbsolutePath(), e);
        }
        videoTrack = null;
        audioTrack = null;
        videoFormat = null;
        audioFormat = null;
        for (Track t : tracks) {
            Format f = t.getFormat();
            if (f instanceof VideoFormat && videoTrack == null) {
                videoTrack = t;
                videoFormat = (VideoFormat) f;
            } else if (f instanceof AudioFormat && audioTrack == null) {
                audioTrack = t;
                audioFormat = (AudioFormat) f;
            }
        }

        // init decoders
        videoFormat = (VideoFormat) videoDecoder.setInputFormat(videoFormat);
        if (videoFormat == null) {
            System.err.println("Error setting input format.");
            return;
        }
        videoDecoder.open();

        audioDecoder.setInputFormat(audioFormat);
        audioDecoder.open();

        videoFrame = new Buffer();
        audioFrame = new Buffer();
        decodedAudioFrame = new Buffer();

        // retrieve one frame to get the buffer sizes
        MediaFrame frame = new MediaFrame(new AudioBuffer(), new VideoBuffer());
        readFrame(frame);
        //Buffer ab = frame.audio.getBuffer();
        /*audioBufferBytes = ab.getLength() - ab.getOffset();*/
        double targetAudioFrames = audioFormat.getSampleRate() / videoFormat.getFrameRate();
        audioBufferBytes = (int) Math.ceil(targetAudioFrames) * audioFormat.getFrameSizeInBits() / 8;
        System.out.println("audio buffer bytes: " + audioBufferBytes);
        Buffer vb = frame.video.getBuffer();
        videoBufferInts = vb.getLength() - vb.getOffset();
        System.out.println("video buffer ints: " + videoBufferInts);

        // DEBUG output
        System.out.println("-------------------------------");
        System.out.println("audio sample size: " + parser.getAudioSampleNumber());
        System.out.println("duration seconds: " + parser.getDuration().getSeconds());
        System.out.println("video track start time seconds: " + videoTrack.getStartTime().getSeconds());
        System.out.println("video track duration seconds: " + videoTrack.getDuration().getSeconds());
        System.out.println("audio track start time seconds: " + audioTrack.getStartTime().getSeconds());
        System.out.println("audio track duration seconds: " + audioTrack.getDuration().getSeconds());
        System.out.println("-------------------------------");
        printVideoFormat(videoFormat);
        printAudioFormat(audioFormat);
        printBufferInfo(vb, "video");
        //printBufferInfo(ab, "audio");

        // reset again
        parser.reset();
    }

    private void printVideoFormat(VideoFormat videoFormat) {
        System.out.println("--- Video format");
        //System.out.println("data type: " + videoFormat.getDataType().getName());
        //System.out.println("encoding: " + videoFormat.getEncoding());
        System.out.println("frame rate: " + videoFormat.getFrameRate());
        System.out.println("max data length: " + videoFormat.getMaxDataLength());
        System.out.println("video picture size: " + videoFormat.getSize());
        System.out.println("-------------------------------");
    }

    private void printAudioFormat(AudioFormat audioFormat) {
        System.out.println("--- Audio format");
        //System.out.println("data type: " + audioFormat.getDataType().getName());
        //System.out.println("encoding: " + audioFormat.getEncoding());
        System.out.println("channels: " + audioFormat.getChannels());
        System.out.println("frame size in bits: " + audioFormat.getFrameSizeInBits());
        System.out.println("sample rate: " + audioFormat.getSampleRate());
        System.out.println("signed/unsigned: "
                + (audioFormat.getSigned() == AudioFormat.SIGNED ? "SIGNED" : "UNSIGNED"));
        System.out.println("endian: "
                + (audioFormat.getEndian() == AudioFormat.BIG_ENDIAN ? "BIG_ENDIAN" : "LITTLE_ENDIAN"));
        System.out.println("frame rate: " + audioFormat.getFrameRate());
        System.out.println("-------------------------------");
    }

    private void printBufferInfo(Buffer buffer, String kind) {
        //System.out.println(kind + " frame duration in milliseconds: " + buffer.getDuration());
        System.out.println(kind + " frame flags: " + buffer.getFlags() + " = " + flagsToString(buffer.getFlags()));
        //System.out.println(kind + " frame header: " + buffer.getHeader());
        //System.out.println(kind + " frame sequence number: " + buffer.getSequenceNumber());
        //System.out.println(kind + " frame time stamp in nanoseconds: " + buffer.getTimeStamp());
        System.out.println("-------------------------------");

    }

    //private int DEBUG_counter = 0;

    private String flagsToString(int flags) {
        StringBuilder b = new StringBuilder();
        addFlag(flags, Buffer.FLAG_EOM, "FLAG_EOM", b);
        addFlag(flags, Buffer.FLAG_DISCARD, "FLAG_DISCARD", b);
        addFlag(flags, Buffer.FLAG_SILENCE, "FLAG_SILENCE", b);
        addFlag(flags, Buffer.FLAG_SID, "FLAG_SID", b);
        addFlag(flags, Buffer.FLAG_KEY_FRAME, "FLAG_KEY_FRAME", b);
        addFlag(flags, Buffer.FLAG_NO_DROP, "FLAG_NO_DROP", b);
        addFlag(flags, Buffer.FLAG_NO_WAIT, "FLAG_NO_WAIT", b);
        addFlag(flags, Buffer.FLAG_NO_SYNC, "FLAG_NO_SYNC", b);
        addFlag(flags, Buffer.FLAG_RELATIVE_TIME, "FLAG_RELATIVE_TIME", b);
        addFlag(flags, Buffer.FLAG_FLUSH, "FLAG_FLUSH", b);
        addFlag(flags, Buffer.FLAG_SYSTEM_MARKER, "FLAG_SYSTEM_MARKER", b);
        addFlag(flags, Buffer.FLAG_RTP_MARKER, "FLAG_RTP_MARKER", b);
        String str = b.toString();
        // remove " | " at the beginning
        if (str.length() > 3) {
            return str.substring(3);
        } else {
            return str;
        }
    }

    private void addFlag(int flags, int flag, String flagName, StringBuilder b) {
        if ((flag & flags) != 0) {
            b.append(" | ");
            b.append(flagName);
        }
    }

    // TODO find a way to handle different frame rates (e.g. video 15 frames per second, audio 88200 frames per second)

    public void readFrame(MediaFrame frame) {
        Buffer decodedVideoFrame = frame.video.getBuffer();
        videoTrack.readFrame(videoFrame);

        int videoResult = videoDecoder.process(videoFrame, decodedVideoFrame);

        Buffer targetBuffer = frame.audio.getBuffer();
        targetBuffer.setLength(0);
        targetBuffer.setOffset(0);
        int audioResult = 0;
        while (targetBuffer.getLength() < audioBufferBytes && audioResult == 0) {
            // if there are no bytes left for copying we fetch a new buffer of data
            if (decodedAudioFrameAvailable == 0) {
                audioTrack.readFrame(audioFrame);
                audioResult = audioDecoder.process(audioFrame, decodedAudioFrame);
                decodedAudioFrameAvailable = decodedAudioFrame.getLength();
                if (decodedAudioFrameAvailable == 0) {
                    System.err.println("Warning: It seems like there is no audio data left.");
                    break;
                }
            }
            copySourceAudioFrame(targetBuffer);
        }

        if (videoResult != 0 || audioResult != 0) {
            System.out.println("results " + videoResult + " " + audioResult);
        }

        if (/*DEBUG_counter++ < 10 ||*/frame.video.getBuffer().getFlags() != 0
                || frame.audio.getBuffer().getFlags() != 0) {
            printBufferInfo(frame.video.getBuffer(), "video");
            printBufferInfo(frame.audio.getBuffer(), "audio");
        }
    }

    private void copySourceAudioFrame(Buffer targetBuffer) {
        byte[] data = (byte[]) targetBuffer.getData();
        int offset = targetBuffer.getLength();
        int length = Math.min(decodedAudioFrameAvailable, audioBufferBytes - offset);
        System.arraycopy(decodedAudioFrame.getData(), decodedAudioFrame.getOffset(), data, offset, length);
        targetBuffer.setLength(offset + length);
        decodedAudioFrameAvailable -= length;
    }

    public double setPosition(double seconds) {
        Time actualPosition = parser.setPosition(new Time(seconds), 0);
        decodedAudioFrameAvailable = 0;
        return actualPosition.getSeconds();
    }

    public double getPosition() {
        return parser.getMediaTime().getSeconds();
    }

    public VideoFormat getVideoFormat() {
        return videoFormat;
    }

    public javax.sound.sampled.AudioFormat getAudioFormat() {
        return convertToJavaSound(audioFormat);
    }

    private static javax.sound.sampled.AudioFormat convertToJavaSound(AudioFormat f) {
        float sampleRate = (float) f.getSampleRate();
        int sampleSize = f.getSampleSizeInBits();
        int channels = f.getChannels();
        boolean signed = f.getSigned() == AudioFormat.SIGNED;
        boolean bigEndian = f.getEndian() == AudioFormat.BIG_ENDIAN;
        return new javax.sound.sampled.AudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
    }

    public MediaFrame createFrameBuffer() {
        AudioBuffer audioFrame = new AudioBuffer();
        audioFrame.getBuffer().setData(new byte[audioBufferBytes]);
        VideoBuffer videoFrame = new VideoBuffer();
        videoFrame.getBuffer().setData(new int[videoBufferInts]);
        return new MediaFrame(audioFrame, videoFrame);
    }

    public void close() {
        // close decoders
        videoDecoder.close();
        audioDecoder.close();

        // close parser
        parser.stop();
        parser.close();

        // close data source
        fileDataSource.disconnect();
    }
}
