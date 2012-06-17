/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.10.2010
 */
package figurabia.ui.video.access;

public class MediaFrame {
    public final AudioBuffer audio;
    public final VideoBuffer video;
    boolean endOfMedia;
    double timestamp;

    public MediaFrame(AudioBuffer audio, VideoBuffer video) {
        this.audio = audio;
        this.video = video;
    }

    /*public double getPositionInSeconds(long frameNumber) {
        VideoFormat vf = (VideoFormat) this.video.getBuffer().getFormat();
        return frameNumber / vf.getFrameRate();
    }*/

    public boolean isEndOfMedia() {
        return endOfMedia;
    }

    /**
     * Only call this method, if you can be sure that this frame will no longer be used.
     */
    public void delete() {
        video.videoPicture.delete();
    }

    /**
     * @return the timestamp in milliseconds
     */
    public double getTimestamp() {
        return timestamp;
    }
}