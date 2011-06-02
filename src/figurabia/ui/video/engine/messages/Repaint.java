/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.04.2011
 */
package figurabia.ui.video.engine.messages;

import figurabia.ui.video.engine.VideoScreen;

public class Repaint {

    public final VideoScreen screen;

    public Repaint(VideoScreen screen) {
        this.screen = screen;
    }
}
