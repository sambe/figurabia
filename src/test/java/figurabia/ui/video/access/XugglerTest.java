/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on May 6, 2012
 */
package figurabia.ui.video.access;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;

import figurabia.experiment.gui.SoundViewer;

public class XugglerTest {

    private static final long END_OF_MEDIA = -541478725L;

    //File file = new File("/home/sberner/Desktop/10-07.04.09.flv");
    //File file = new File("/home/sberner/Desktop/10-21.04.09.flv");
    //File file = new File("/home/sberner/Desktop/10-31.03.09.flv");
    //File file = new File("/home/sberner/media/salsavids/m2/MOV00357.MP4");
    //File file = new File("/home/sberner/Desktop/10-07.04.09.flv");
    File file = new File("/home/sberner/media/salsavids/m2/MOV00356.MP4");

    //File file = new File("/home/sberner/media/salsavids/salsabrosa/10-08.07.08_.flv");

    private IContainer container;
    private IStream[] streams = null;
    private IStreamCoder[] coders = null;
    private IStream audioStream = null;
    private IStream videoStream = null;
    private IStreamCoder audioCoder = null;
    private IStreamCoder videoCoder = null;

    @Before
    public void setup() throws Exception {
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
            } else if (coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
                videoStream = stream;
                videoCoder = coder;
            } else {
                System.err.println("Unknown codec of type " + coder.getCodecType());
            }
        }

        if (audioCoder.open(null, null) < 0)
            throw new RuntimeException("error opening audio stream coder");
        if (videoCoder.open(null, null) < 0)
            throw new RuntimeException("error opening video stream coder");
    }

    @After
    public void tearDown() throws Exception {
        if (audioCoder != null && audioCoder.isOpen())
            audioCoder.close();
        if (videoCoder != null && videoCoder.isOpen())
            videoCoder.close();
        if (container != null && container.isOpened()) {
            container.close();
        }
    }

    @Test
    public void testPacketLevel() throws Exception {
        //double videoPtsBase = videoCoder.getTimeBase().getDouble() * 1000;
        double duration = container.getDuration() / 1000000;
        System.out.println("Video duration: " + duration);
        System.out.println("Video timebase: " + videoCoder.getTimeBase());
        System.out.println("Audio timebase: " + audioCoder.getTimeBase());
        int n = 1000;
        IPacket[] packets = new IPacket[1000];
        byte[][] packetData = new byte[packets.length][];
        for (int i = 0; i < packets.length; i++)
            packets[i] = IPacket.make();

        // TODO checking if the following makes a difference
        container.seekKeyFrame(-1, -5000000, -50000, 0, 0);

        for (int i = 0; i < packets.length; i++) {
            int errorNum;
            if ((errorNum = container.readNextPacket(packets[i])) < 0) {
                if (errorNum == END_OF_MEDIA) {
                    n = i;
                    break;
                } else {
                    IError error = IError.make(errorNum);
                    throw new RuntimeException("error reading packet: " + error.getDescription());
                }
            }
            int size = packets[i].getSize();
            packetData[i] = new byte[size];
            packets[i].get(0, packetData[i], 0, size);
        }

        // display attributes
        for (int i = 0; i < n; i++) {
            String type = packets[i].getStreamIndex() == audioStream.getIndex() ? "A"
                    : (packets[i].getStreamIndex() == videoStream.getIndex() ? "V" : "X");
            double base = packets[i].getTimeBase().getValue() * 1000;
            double ts = packets[i].getTimeStamp() * base;
            double pts = packets[i].getPts() * base;
            double dts = packets[i].getDts() * base;
            System.out.println(i + ": " + packets[i].getPosition() + ", " + ts + ", " + dts + ", " + pts + " " + type
                    + (packets[i].isKey() ? " K" : "") + " " + packets[i].getSize());
        }

        container.seekKeyFrame(-1, 0, 0);

        // read same part again
        for (int i = 0; i < n; i++) {
            IPacket newPacket = IPacket.make();
            if (container.readNextPacket(newPacket) < 0)
                throw new RuntimeException("error reading packet");
            int size = newPacket.getSize();
            byte[] data = new byte[size];
            newPacket.get(0, data, 0, size);
            Assert.assertArrayEquals(packetData[i], data);
        }

        // prove that it also works if seeking to any position
        int lastSeeked = n;
        boolean videoKeyFrameFound = false;
        long pts = -1;
        for (int i = n - 1; i >= 0; i--) {
            if (!videoKeyFrameFound && (packets[i].getStreamIndex() == audioStream.getIndex() || !packets[i].isKey())) {
                continue;
            } else {
                if (!videoKeyFrameFound) {
                    pts = (int) (packets[i].getPts() * 1000);
                    videoKeyFrameFound = true;
                }
                // continuing to skip back until the audio frames before the video key frame have been regarded too
                int prevAudio = i - 1;
                while (prevAudio >= 0 && packets[prevAudio].getStreamIndex() != audioStream.getIndex())
                    prevAudio--;
                if (prevAudio >= 0 && packets[prevAudio].getStreamIndex() == audioStream.getIndex()) {
                    long audioPts = (long) (packets[prevAudio].getPts()
                            * coders[packets[prevAudio].getStreamIndex()].getTimeBase().getValue() * 1000000);
                    if (audioPts > pts)
                        continue;
                    else
                        i = prevAudio; // manipulate loop pointer to be at the audio frame just before the video key frame
                }
            }
            videoKeyFrameFound = false; //reset for next use
            System.out.println(i + ": setting to pts " + pts);
            if (container.seekKeyFrame(-1, pts - 5000000, pts, pts, 0) < 0)
                throw new RuntimeException("Seeking failed");

            for (int j = i; j < lastSeeked; j++) {
                IPacket newPacket = IPacket.make();
                int statusCode;
                if ((statusCode = container.readNextPacket(newPacket)) < 0) {
                    IError error = IError.make(statusCode);
                    throw new RuntimeException("error reading packet: " + error.getDescription());
                }

                String type = newPacket.getStreamIndex() == audioStream.getIndex() ? "A"
                        : (newPacket.getStreamIndex() == videoStream.getIndex() ? "V" : "X");
                //System.out.println(j + ": " + newPacket.getPosition() + ", " + newPacket.getDts() + ", "
                //        + newPacket.getPts() + " " + type + (newPacket.isKey() ? " K" : ""));

                if (!videoKeyFrameFound) {
                    while (j < lastSeeked && packets[j].getStreamIndex() == videoStream.getIndex()
                            && !packets[j].isKey())
                        j++; // skip video non-keyframes before key frame
                    if (packets[j].getStreamIndex() == videoStream.getIndex() && packets[j].isKey())
                        videoKeyFrameFound = true;
                }

                int size = newPacket.getSize();
                byte[] data = new byte[size];
                newPacket.get(0, data, 0, size);
                Assert.assertArrayEquals("difference in packet " + j, packetData[j], data);
            }
            lastSeeked = i;
            videoKeyFrameFound = false;
        }

    }

    @Test
    public void testDecodedLevel() throws Exception {

        // 1) get the first 1000 packets
        int n = Math.min(1000, (int) (container.getDuration() / 1000000 * videoCoder.getFrameRate().getDouble()));
        IPacket[] packets = new IPacket[n];
        byte[][] packetData = new byte[packets.length][];
        for (int i = 0; i < packets.length; i++)
            packets[i] = IPacket.make();

        for (int i = 0; i < packets.length; i++) {
            int errorNum;
            if ((errorNum = container.readNextPacket(packets[i])) < 0) {
                IError error = IError.make(errorNum);
                throw new RuntimeException("error reading packet: " + error.getDescription());
            }
            int size = packets[i].getSize();
            packetData[i] = new byte[size];
            packets[i].get(0, packetData[i], 0, size);
        }

        // 2a) process all audio packets
        short[][] audioData = new short[packets.length][];
        IAudioSamples audioSamples = IAudioSamples.make(1024, audioCoder.getChannels());
        //prewarmDecoder(audioSamples, packets[327]);
        //prewarmDecoder(audioSamples, packets[328]);
        //prewarmDecoder(audioSamples, packets[329]);
        for (int i = 0; i < packets.length; i++) {
            if (packets[i].getStreamIndex() != audioStream.getIndex())
                continue;
            //System.out.println("processing now!!! " + i);

            int offset = 0;
            while (offset < packets[i].getSize()) {
                int bytesDecoded = audioCoder.decodeAudio(audioSamples, packets[i], offset);
                if (bytesDecoded < 0)
                    throw new RuntimeException("error decoding bytes");
                offset += bytesDecoded;

                if (audioSamples.isComplete()) {
                    // copy the data
                    Assert.assertNull(audioData[i]);
                    audioData[i] = new short[audioSamples.getSize() / 2];
                    audioSamples.get(0, audioData[i], 0, audioData[i].length);
                }
            }
        }

        // 2b) process all video packets

        // 3a) process audio again, and check if it's still the same
        for (int i = 0; i < packets.length; i++) {
            if (packets[i].getStreamIndex() != audioStream.getIndex())
                continue;

            int offset = 0;
            while (offset < packets[i].getSize()) {
                int bytesDecoded = audioCoder.decodeAudio(audioSamples, packets[i], offset);
                if (bytesDecoded < 0)
                    throw new RuntimeException("error decoding bytes");
                offset += bytesDecoded;

                if (audioSamples.isComplete()) {
                    // copy the data
                    int size = audioSamples.getSize();
                    short[] array = new short[size / 2];
                    audioSamples.get(0, array, 0, array.length);

                    if (i < 3) // don't check first frames because is is influenced by the unknown frame that was decoded just before (for some videos it is up to 3 frames far)
                        continue; // check if it was the same as before
                    Assert.assertEquals("audio data size different", audioData[i].length, array.length);
                    for (int j = 0; j < array.length; j++) {

                        boolean same = Math.abs(audioData[i][j] - array[j]) <= 1;
                        if (!same) {
                            //SoundUtil.playSound(audioData[i]);
                            //SoundUtil.playSound(array);
                            SoundViewer.displayViewer(Arrays.copyOf(audioData[i], 200), Arrays.copyOf(array, 200));
                            Assert.fail("packet " + i + ": audio difference at byte position " + (j * 2)
                                    + " with the values: "
                                    + audioData[i][j] + " and " + array[j]);
                            break;
                        }
                    }
                }
            }
        }

    }

    @Test
    public void testManualSeekingSpeed() {
        IPacket packet = IPacket.make();
        int nAudio = 0;
        int nVideo = 0;
        int nOthers = 0;
        long startNanos = System.nanoTime();
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
            if (packet.getStreamIndex() == audioStream.getIndex())
                nAudio++;
            else if (packet.getStreamIndex() == videoStream.getIndex())
                nVideo++;
            else
                nOthers++;
            int size = packet.getSize();
            long position = packet.getPosition();
            long pts = packet.getPts();
            long dts = packet.getDts();
            boolean key = packet.isKey();
        }
        long stopNanos = System.nanoTime();
        double totalMS = (stopNanos - startNanos) / 1000000.0;

        System.out.println("Total # of frames: " + nAudio + " audio, " + nVideo + " video, " + nOthers + " others");
        System.out.println("Total time taken: " + totalMS + "ms");
    }

    private void prewarmDecoder(IAudioSamples audioSamples, IPacket packet) {
        int offset = 0;
        while (offset < packet.getSize()) {
            int bytesDecoded = audioCoder.decodeAudio(audioSamples, packet, offset);
            if (bytesDecoded < 0)
                throw new RuntimeException("error decoding bytes");
            offset += bytesDecoded;
        }
    }
}
