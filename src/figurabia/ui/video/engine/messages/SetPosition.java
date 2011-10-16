/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 07.05.2011
 */
package figurabia.ui.video.engine.messages;

public class SetPosition {

    public final long position;
    public final boolean animated;

    public SetPosition(long position) {
        this(position, false);
    }

    public SetPosition(long position, boolean animated) {
        this.position = position;
        this.animated = animated;
    }
}
