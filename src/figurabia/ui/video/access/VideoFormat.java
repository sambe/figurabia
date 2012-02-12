/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 02.01.2012
 */
package figurabia.ui.video.access;

import java.awt.Dimension;

public class VideoFormat {

    private final String encoding;
    private final Dimension size;
    private final double frameRate;

    public VideoFormat(String encoding, Dimension size, double frameRate) {
        super();
        this.encoding = encoding;
        this.size = size;
        this.frameRate = frameRate;
    }

    /**
     * @return the encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * @return the size
     */
    public Dimension getSize() {
        return size;
    }

    /**
     * @return the frameRate
     */
    public double getFrameRate() {
        return frameRate;
    }
}
