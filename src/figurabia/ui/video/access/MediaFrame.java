/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.10.2010
 */
package figurabia.ui.video.access;

import javax.media.format.VideoFormat;

public class MediaFrame {
    public long frameNumber;
    public final AudioBuffer audio;
    public final VideoBuffer video;

    public MediaFrame(AudioBuffer audio, VideoBuffer video) {
        this.audio = audio;
        this.video = video;
    }

    public double getPositionInSeconds() {
        VideoFormat vf = (VideoFormat) this.video.getBuffer().getFormat();
        return frameNumber / vf.getFrameRate();
    }

    /**
     * @return the frameNumber
     */
    public long getFrameNumber() {
        return frameNumber;
    }

    /**
     * @param frameNumber the frameNumber to set
     */
    public void setFrameNumber(long frameNumber) {
        this.frameNumber = frameNumber;
    }
}
