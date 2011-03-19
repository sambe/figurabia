/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 30.01.2011
 */
package figurabia.ui.video.engine.messages;

import figurabia.ui.video.engine.actorframework.Actor;

public class FrameRequest extends ResponseRequest {

    public final long seqNum;

    public FrameRequest(long seqNum, Actor responseTo) {
        super(responseTo);
        this.seqNum = seqNum;
    }
}
