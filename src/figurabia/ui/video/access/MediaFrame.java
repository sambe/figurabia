/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.10.2010
 */
package figurabia.ui.video.access;

import javax.media.format.VideoFormat;

public class MediaFrame {
    public final AudioBuffer audio;
    public final VideoBuffer video;
    boolean endOfMedia;

    public MediaFrame(AudioBuffer audio, VideoBuffer video) {
        this.audio = audio;
        this.video = video;
    }

    public double getPositionInSeconds(long frameNumber) {
        VideoFormat vf = (VideoFormat) this.video.getBuffer().getFormat();
        return frameNumber / vf.getFrameRate();
    }

    public boolean isEndOfMedia() {
        return endOfMedia;
    }
}
