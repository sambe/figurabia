/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine.messages;

import java.io.File;

public class NewVideo {

    public final File videoFile;
    public final Long initialPosition; // in milliseconds
    public final Long positionMin;
    public final Long positionMax;

    public NewVideo(File mediaFile) {
        this(mediaFile, null);
    }

    public NewVideo(File mediaFile, Long initialPosition) {
        this(mediaFile, initialPosition, null, null);
    }

    public NewVideo(File mediaFile, Long positionMin, Long positionMax) {
        this(mediaFile, null, positionMin, positionMax);
    }

    public NewVideo(File mediaFile, Long initialPosition, Long positionMin, Long positionMax) {
        this.videoFile = mediaFile;
        this.initialPosition = initialPosition;
        this.positionMin = positionMin;
        this.positionMax = positionMax;
    }

}
