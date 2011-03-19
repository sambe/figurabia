/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine.messages;

import java.io.File;

public class NewMedia {

    public final File mediaFile;

    public NewMedia(File mediaFile) {
        this.mediaFile = mediaFile;
    }

}
