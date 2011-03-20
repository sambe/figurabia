/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine;

import java.io.File;

import javax.media.format.VideoFormat;
import javax.sound.sampled.AudioFormat;

import figurabia.ui.video.access.MediaInputStream;
import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.messages.CachedFrame;
import figurabia.ui.video.engine.messages.FetchFrames;
import figurabia.ui.video.engine.messages.MediaInfoRequest;
import figurabia.ui.video.engine.messages.MediaInfoResponse;
import figurabia.ui.video.engine.messages.CachedFrame.CachedFrameState;

public class FrameFetcher extends Actor {

    private MediaInputStream mediaInputStream;
    private final File mediaFile;
    private double frameRate = 0;

    public FrameFetcher(Actor errorHandler, File mediaFile) {
        super(errorHandler);
        this.mediaFile = mediaFile;
    }

    @Override
    protected void init() throws Exception {
        mediaInputStream = new MediaInputStream(mediaFile);
        frameRate = mediaInputStream.getVideoFormat().getFrameRate();
    }

    @Override
    protected void destruct() {
        mediaInputStream.close();
    }

    @Override
    protected void act(Object message) {
        if (message instanceof FetchFrames) {
            handleFetchFrames((FetchFrames) message);
        } else if (message instanceof MediaInfoRequest) {
            handleMediaInfoRequest((MediaInfoRequest) message);
        } else {
            throw new IllegalStateException("received unknown message");
        }
    }

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

    private void handleMediaInfoRequest(MediaInfoRequest message) {
        VideoFormat videoFormat = mediaInputStream.getVideoFormat();
        AudioFormat audioFormat = mediaInputStream.getAudioFormat();
        MediaInfoResponse response = new MediaInfoResponse(videoFormat, audioFormat);
        message.responseTo.send(response);
    }
}
