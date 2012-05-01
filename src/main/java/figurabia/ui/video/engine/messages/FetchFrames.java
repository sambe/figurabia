/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine.messages;

import figurabia.ui.video.engine.actorframework.MessageSendable;

public class FetchFrames extends ResponseRequest {

    public final CacheBlock block;

    public FetchFrames(MessageSendable responseTo, CacheBlock block) {
        super(responseTo);
        this.block = block;
    }
}
