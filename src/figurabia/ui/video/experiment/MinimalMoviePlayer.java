/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 26.09.2010
 */
package figurabia.ui.video.experiment;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;

import javax.media.Buffer;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.Time;
import javax.media.Track;
import javax.media.format.AudioFormat;
import javax.media.format.RGBFormat;
import javax.media.format.VideoFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.omnividea.media.parser.video.Parser;
import com.omnividea.media.protocol.file.DataSource;

import figurabia.ui.util.SimplePanelFrame;

public class MinimalMoviePlayer {

    private static volatile long newLocationToSet = -1;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        MediaLocator mediaLocator = new MediaLocator("file:/home/sberner/Desktop/10-21.04.09.flv");
        DataSource fileDataSource = new DataSource();
        fileDataSource.setLocator(mediaLocator);
        fileDataSource.connect();

        Parser parser = new Parser();

        com.omnividea.media.codec.video.NativeDecoder videoDecoder = new com.omnividea.media.codec.video.NativeDecoder();

        com.omnividea.media.codec.audio.NativeDecoder audioDecoder = new com.omnividea.media.codec.audio.NativeDecoder();

        // init parser
        parser.setSource(fileDataSource);
        parser.open();
        parser.start();
        Track[] tracks = parser.getTracks();
        Track videoTrack = null;
        Track audioTrack = null;
        VideoFormat videoFormat = null;
        AudioFormat audioFormat = null;
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

        // create screen
        JPanel screen = new JPanel();
        screen.setLayout(null);
        int width = videoFormat.getSize().width;
        int height = videoFormat.getSize().height;
        JButton setPositionButton = new JButton("Set Position");
        setPositionButton.setBounds(10, height + 10, 180, 30);
        screen.add(setPositionButton);
        SimplePanelFrame frame = new SimplePanelFrame(screen, width + 20, height + 20 + 65);

        // add action on button click
        setPositionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newLocationToSet = 10000;
            }
        });

        // create sound channel
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, convertToJavaSound(audioFormat));
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(convertToJavaSound(audioFormat));
        line.start();

        // get frames (TODO handle flags in buffers)
        Buffer videoFrame = new Buffer();
        Buffer[] videoRawFrames = new Buffer[100];
        for (int i = 0; i < videoRawFrames.length; i++) {
            videoRawFrames[i] = new Buffer();
        }
        Buffer audioFrame = new Buffer();
        Buffer[] audioRawFrames = new Buffer[100];
        for (int i = 0; i < audioRawFrames.length; i++) {
            audioRawFrames[i] = new Buffer();
        }
        for (int i = 0; i < 100; i++) {

            if (newLocationToSet != -1) {
                Time actualPosition = parser.setPosition(new Time(newLocationToSet * 1000000L), 0);
                System.out.println("Set to " + actualPosition.getSeconds() + " seconds");
                newLocationToSet = -1;
            }

            // preprocess frames
            for (int j = 0; j < 100; j++) {
                videoTrack.readFrame(videoFrame);
                if (j != 0 && videoRawFrames[j].getData() == null) {
                    int size = ((int[]) videoRawFrames[j - 1].getData()).length;
                    videoRawFrames[j].setData(new int[size]);
                }
                int videoResult = videoDecoder.process(videoFrame, videoRawFrames[j]);

                audioTrack.readFrame(audioFrame);
                int audioResult = audioDecoder.process(audioFrame, audioRawFrames[j]);

                if (videoResult != 0 || audioResult != 0) {
                    System.out.println("results(" + (i * 100 + j) + ") " + videoResult + " " + audioResult);
                }

                // reverse audio
                int samplesize = ((AudioFormat) audioRawFrames[j].getFormat()).getSampleSizeInBits() / 8;
                int offset = audioRawFrames[j].getOffset();
                int length = audioRawFrames[j].getLength();
                byte[] audioData = (byte[]) audioRawFrames[j].getData();
                for (int k = offset; k * 2 < length; k += samplesize) {
                    int k2 = length - k - samplesize;
                    for (int l = 0; l < samplesize; l++) {
                        byte tmp = audioData[k + l];
                        audioData[k + l] = audioData[k2 + l];
                        audioData[k2 + l] = tmp;
                    }
                }
            }

            // play frames backwards
            for (int j = 99; j >= 0; j--) {
                // display a video frame
                Graphics g = screen.getGraphics();
                g.drawImage(bufferToImage(videoRawFrames[j]), 0, 0, width, height, null);

                // play an audio frame
                int bytesWritten = line.write((byte[]) audioRawFrames[j].getData(), audioRawFrames[j].getOffset(),
                        audioRawFrames[j].getLength());
                System.out.println("written " + bytesWritten + " bytes to audio line");
                /*if (i != 0) {
                    //line.drain();
                    long nextFrameTime = audioFrame.getTimeStamp() / 1000;
                    long currentTime = line.getMicrosecondPosition();
                    //System.out.println("nextFrameTime = " + nextFrameTime + ";  currentTime = " + currentTime);
                    while (currentTime + 100 < nextFrameTime) {
                        Thread.sleep(1);
                        currentTime = line.getMicrosecondPosition();
                    }
                }*/
                Thread.sleep(20);
            }
        }

        // was checking out how this would work inside (to estimate if it is expensive to call it often)
        //parser.setPosition(new Time(5.0), 0);
        // other goals: trying to find out, what kind of data is passed around and in what amounts (to find a suitable caching strategy)

        // close screen
        frame.setVisible(false);

        // close sound channel
        line.close();

        // close decoders
        videoDecoder.close();
        audioDecoder.close();

        // close parser
        parser.stop();
        parser.close();
    }

    private static javax.sound.sampled.AudioFormat convertToJavaSound(AudioFormat f) {
        //Encoding encoding = new Encoding(f.getEncoding());
        float sampleRate = (float) f.getSampleRate();
        int sampleSize = f.getSampleSizeInBits();
        int channels = f.getChannels();
        int frameSize = f.getFrameSizeInBits();
        float frameRate = (float) f.getFrameRate();
        boolean signed = f.getSigned() == AudioFormat.SIGNED;
        boolean bigEndian = f.getEndian() == AudioFormat.BIG_ENDIAN;
        return new javax.sound.sampled.AudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
    }

    private static BufferedImage bufferToImage(Buffer buffer) {
        RGBFormat format = (RGBFormat) buffer.getFormat();
        int rMask, gMask, bMask;
        Object data = buffer.getData();
        DirectColorModel dcm;

        rMask = format.getRedMask();
        gMask = format.getGreenMask();
        bMask = format.getBlueMask();
        int[] masks = new int[3];
        masks[0] = rMask;
        masks[1] = gMask;
        masks[2] = bMask;

        DataBuffer db = new DataBufferInt((int[]) data, format.getLineStride() * format.getSize().height);

        SampleModel sm = new SinglePixelPackedSampleModel(DataBuffer.TYPE_INT, format.getLineStride(),
                format.getSize().height, masks);
        WritableRaster wr = Raster.createWritableRaster(sm, db, new Point(0, 0));

        dcm = new DirectColorModel(24, rMask, gMask, bMask);
        return new BufferedImage(dcm, wr, true, null);
    }
}
