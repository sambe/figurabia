/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 05.06.2011
 */
package figurabia.ui.video.engine.messages;

public class AudioSyncEvent {

    public static enum Type {
        START,
        STOP;
    }

    public final Type type;

    public AudioSyncEvent(Type type) {
        this.type = type;
    }
}
