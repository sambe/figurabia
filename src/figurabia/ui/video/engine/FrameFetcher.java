/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine;

import figurabia.ui.video.access.MediaInputStream;
import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.messages.CachedFrame;
import figurabia.ui.video.engine.messages.FetchFrames;
import figurabia.ui.video.engine.messages.CachedFrame.CachedFrameState;

public class FrameFetcher extends Actor {

    private final MediaInputStream mediaInputStream;
    private double frameRate = 0;

    public FrameFetcher(Actor errorHandler, MediaInputStream mediaInputStream) {
        super(errorHandler);
        this.mediaInputStream = mediaInputStream;
    }

    @Override
    protected void act(Object message) {
        /*if (message instanceof NewMedia) {
            handleNewMedia((NewMedia) message);
        } else*/if (message instanceof FetchFrames) {
            handleFetchFrames((FetchFrames) message);
        } /*else if (message instanceof CreateFrameBuffers) {
            handleCreateFrameBuffers((CreateFrameBuffers) message);
          }*/else {
            throw new IllegalStateException("received unknown message");
        }
    }

    /*private void handleCreateFrameBuffers(CreateFrameBuffers message) {
        List<MediaFrame> frameBuffers = new ArrayList<MediaFrame>(message.number);
        for (int i = 0; i < message.number; i++) {
            frameBuffers.add(mediaInputStream.createFrameBuffer());
        }
        message.responseTo.send(new CreateFrameBuffersResponse(frameBuffers));
    }*/

    private void handleFetchFrames(FetchFrames message) {

        // set position and find seq nr
        double newPosition;
        if (message.startSeqNr != -1) {
            double position = message.startSeqNr / frameRate;
            newPosition = mediaInputStream.setPosition(position);
        } else {
            newPosition = mediaInputStream.getPosition();
        }
        long startSeqNr = Math.round(newPosition * frameRate);

        // read frames from stream
        for (CachedFrame f : message.frames) {
            // allocating a buffer if the cachedFrame does not bring one already
            if (f.frame == null) {
                f.frame = mediaInputStream.createFrameBuffer();
            }
            f.state = CachedFrameState.FETCHING;
            mediaInputStream.readFrame(f.frame);
            f.seqNum = startSeqNr++;
        }
        message.responseTo.send(message);
    }

    // idea is to create a new actor instead
    /*private void handleNewMedia(NewMedia message) {
        if (mediaInputStream != null) {
            mediaInputStream.close();
        }
        try {
            mediaInputStream = new MediaInputStream(message.mediaFile);
            frameRate = mediaInputStream.getVideoFormat().getFrameRate();
        } catch (IOException e) {
            handleException("Error opening media file", e);
        }
    }*/
}
