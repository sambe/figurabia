/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine.messages;

import java.io.File;

public class NewVideo {

    public final File videoFile;
    public long initialPosition; // in milliseconds

    public NewVideo(File mediaFile, long initialPosition) {
        this.videoFile = mediaFile;
        this.initialPosition = initialPosition;
    }

}
