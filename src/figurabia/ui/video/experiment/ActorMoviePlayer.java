/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 26.09.2010
 */
package figurabia.ui.video.experiment;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.media.Buffer;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JPanel;

import figurabia.ui.util.SimplePanelFrame;
import figurabia.ui.video.access.AudioBuffer;
import figurabia.ui.video.access.VideoBuffer;
import figurabia.ui.video.engine.FrameCache;
import figurabia.ui.video.engine.FrameFetcher;
import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.actorframework.ObjectReceiver;
import figurabia.ui.video.engine.messages.CachedFrame;
import figurabia.ui.video.engine.messages.FrameRequest;
import figurabia.ui.video.engine.messages.MediaError;
import figurabia.ui.video.engine.messages.MediaInfoRequest;
import figurabia.ui.video.engine.messages.MediaInfoResponse;

public class ActorMoviePlayer {

    private static volatile long newLocationToSet = -1;

    private static final int FRAME_BATCH = 20;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        Actor errorHandler = new Actor(null) {
            @Override
            protected void act(Object message) {
                if (message instanceof MediaError) {
                    MediaError me = (MediaError) message;
                    System.err.println("A media error occured: " + me.message);
                    me.exception.printStackTrace();
                } else {
                    System.err.println("A unknown type of error occured: " + message);
                }
            }
        };
        FrameFetcher frameFetcher = new FrameFetcher(errorHandler, new File("/home/sberner/Desktop/10-21.04.09.flv"));
        FrameCache frameCache = new FrameCache(errorHandler, frameFetcher);

        errorHandler.start();
        frameFetcher.start();
        frameCache.start();
        ObjectReceiver receiver = new ObjectReceiver();
        frameFetcher.send(new MediaInfoRequest(receiver));
        final MediaInfoResponse mir = (MediaInfoResponse) receiver.waitForMessage();

        // create screen
        JPanel screen = new JPanel();
        screen.setLayout(null);
        int width = mir.videoFormat.getSize().width;
        int height = mir.videoFormat.getSize().height;
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
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, mir.audioFormat);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(mir.audioFormat);
        line.start();

        // get frames
        long seqnum = 0;
        for (int i = 0; i < 100; i++) {

            if (newLocationToSet != -1) {
                seqnum = newLocationToSet;
                System.out.println("Set to seqnum " + seqnum);
                newLocationToSet = -1;
            }

            // retrieve frames
            ObjectReceiver frameReceiver = new ObjectReceiver(FRAME_BATCH);
            for (int j = 0; j < FRAME_BATCH; j++) {
                frameCache.send(new FrameRequest(seqnum++, frameReceiver));
            }
            List<CachedFrame> requestedFrames = frameReceiver.waitForAllMessages(CachedFrame.class);

            // preprocess frames
            for (int j = 0; j < FRAME_BATCH; j++) {
                AudioBuffer audioBuffer = requestedFrames.get(j).frame.audio;
                Buffer buffer = audioBuffer.getBuffer();
                // reverse audio
                int samplesize = mir.audioFormat.getSampleSizeInBits() / 8;
                int offset = buffer.getOffset();
                int length = buffer.getLength();
                byte[] audioData = (byte[]) buffer.getData();
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
            for (int j = FRAME_BATCH - 1; j >= 0; j--) {
                VideoBuffer video = requestedFrames.get(j).frame.video;
                AudioBuffer audio = requestedFrames.get(j).frame.audio;

                // display a video frame
                Graphics g = screen.getGraphics();
                g.drawImage(video.getImage(), 0, 0, width, height, null);

                // play an audio frame
                int bytesWritten = line.write((byte[]) audio.getBuffer().getData(), audio.getBuffer().getOffset(),
                        audio.getBuffer().getLength());
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

        // TODO was checking out how this would work inside (to estimate if it is expensive to call it often)
        //parser.setPosition(new Time(5.0), 0);
        // other goals: trying to find out, what kind of data is passed around and in what amounts (to find a suitable caching strategy)

        // close screen
        frame.setVisible(false);

        // close sound channel
        line.close();

        // stop actors
        frameFetcher.stop();
        frameCache.stop();
    }
}
