/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 20.03.2011
 */
package figurabia.ui.video.engine.messages;

import javax.media.format.VideoFormat;
import javax.sound.sampled.AudioFormat;

public class MediaInfoResponse {

    public final VideoFormat videoFormat;
    public final AudioFormat audioFormat;

    public MediaInfoResponse(VideoFormat videoFormat, AudioFormat audioFormat) {
        this.videoFormat = videoFormat;
        this.audioFormat = audioFormat;
    }
}
