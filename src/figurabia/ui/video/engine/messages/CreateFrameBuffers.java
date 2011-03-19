/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine.messages;

import figurabia.ui.video.engine.actorframework.Actor;

public class CreateFrameBuffers extends ResponseRequest {

    public final int number;

    public CreateFrameBuffers(Actor responseTo, int number) {
        super(responseTo);
        this.number = number;
    }
}
