/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.10.2010
 */
package figurabia.ui.video.access;

import javax.media.Buffer;

/**
 * Represents the memory unit that stores the audio of one frame of the media file. This can be passed around between
 * different actors (to recycle buffers and use a controlled amount of memory).
 */
public class AudioBuffer {

    private Buffer buffer;

    public AudioBuffer() {
        buffer = new Buffer();
    }

    public Buffer getBuffer() {
        return buffer;
    }
}
