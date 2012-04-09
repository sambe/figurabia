/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.10.2010
 */
package figurabia.ui.video.access;

import java.awt.Image;
import java.awt.image.BufferedImage;

import com.xuggle.xuggler.IVideoPicture;

/**
 * Represents the memory unit that stores one image of one frame of the media file. This class can be passed around
 * between different actors.
 */
public class VideoBuffer {

    IVideoPicture videoPicture;
    BufferedImage bufferedImage;

    public VideoBuffer() {
    }

    public Image getImage() {
        return bufferedImage;
    }
}
