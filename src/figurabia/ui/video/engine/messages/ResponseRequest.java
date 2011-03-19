/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine.messages;

import figurabia.ui.video.engine.actorframework.Actor;

public abstract class ResponseRequest {

    public final Actor responseTo;

    public ResponseRequest(Actor responseTo) {
        this.responseTo = responseTo;
    }

}
