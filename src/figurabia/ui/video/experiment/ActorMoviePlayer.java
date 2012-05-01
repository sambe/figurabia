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
import figurabia.ui.video.engine.messages.PrefetchRequest;
import figurabia.ui.video.engine.messages.RecyclingBag;

public class ActorMoviePlayer {

    private static volatile long newLocationToSet = -1;

    private static final int BATCH_SIZE = 8;
    private static final int FRAME_BATCH = 3 * BATCH_SIZE;

    private static final int USAGE_COUNT_TO_ADD = 1; // TODO set back to 2 when reenabling audio

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        Actor errorHandler = new Actor(null, -1) {
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

        //AudioRenderer audioRenderer = new AudioRenderer(errorHandler, mir.audioFormat, null);
        //audioRenderer.start();
        //audioRenderer.send(new ControlCommand(Command.START));

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
                newLocationToSet = 50 * BATCH_SIZE;
            }
        });

        // get frames
        long seqnum = 40 * FRAME_BATCH;
        for (int i = 0; i < 40; i++) {

            if (newLocationToSet != -1) {
                seqnum = newLocationToSet;
                System.out.println("Set to seqnum " + seqnum);
                newLocationToSet = -1;
            }

            // retrieve frames
            ObjectReceiver frameReceiver = new ObjectReceiver(FRAME_BATCH);
            for (int j = 0; j < FRAME_BATCH; j++) {
                frameCache.send(new FrameRequest(--seqnum, USAGE_COUNT_TO_ADD, false, frameReceiver));
            }
            List<CachedFrame> requestedFrames = frameReceiver.waitForAllMessages(CachedFrame.class);

            // prefetch next batch of frames
            frameCache.send(new PrefetchRequest(seqnum));

            // preprocess frames
            for (int j = 0; j < FRAME_BATCH; j++) {
                AudioBuffer audioBuffer = requestedFrames.get(j).frame.audio;
                // reverse audio
                int samplesize = mir.audioFormat.getSampleSizeInBits() / 8;
                byte[] audioData = audioBuffer.getAudioData();
                int offset = 0;
                int length = audioData.length;
                for (int k = offset; k < (length - offset) / 2; k += samplesize) {
                    int k2 = length - offset - k - samplesize;
                    // swap two blocks of "samplesize" bytes
                    for (int l = 0; l < samplesize; l++) {
                        byte tmp = audioData[k + l];
                        audioData[k + l] = audioData[k2 + l];
                        audioData[k2 + l] = tmp;
                    }
                }
            }

            // play frames backwards
            for (CachedFrame cf : requestedFrames) {
                VideoBuffer video = cf.frame.video;
                //AudioBuffer audio = requestedFrames.get(j).frame.audio;

                // display a video frame
                Graphics g = screen.getGraphics();
                g.drawImage(video.getImage(), 0, 0, width, height, null);

                // play an audio frame
                //System.err.println("DEBUG: Sent frame " + cf.seqNum);
                //audioRenderer.send(cf);
                Thread.sleep(60);
            }

            // recycle
            for (CachedFrame cf : requestedFrames) {
                frameCache.send(new RecyclingBag(cf));
            }
        }

        // close screen
        frame.setVisible(false);
        frame.dispose();

        // stop actors
        //audioRenderer.stop();
        frameFetcher.stop();
        frameCache.stop();
        errorHandler.stop();

        //System.exit(0);
    }
}
