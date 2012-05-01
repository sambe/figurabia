/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine.messages;

import figurabia.ui.video.engine.actorframework.Actor;

public class MediaError {

    public final Actor source;
    public final String message;
    public final Exception exception;

    public MediaError(Actor source, String message, Exception exception) {
        this.source = source;
        this.message = message;
        this.exception = exception;
    }
}
