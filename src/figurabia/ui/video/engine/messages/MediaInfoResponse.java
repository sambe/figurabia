/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 20.03.2011
 */
package figurabia.ui.video.engine.messages;

import javax.sound.sampled.AudioFormat;

import figurabia.ui.video.access.VideoFormat;

public class MediaInfoResponse {

    public final VideoFormat videoFormat;
    public final AudioFormat audioFormat;
    public final long duration;

    public MediaInfoResponse(VideoFormat videoFormat, AudioFormat audioFormat, long duration) {
        this.videoFormat = videoFormat;
        this.audioFormat = audioFormat;
        this.duration = duration;
    }
}
