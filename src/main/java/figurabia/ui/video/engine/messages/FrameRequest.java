/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 30.01.2011
 */
package figurabia.ui.video.engine.messages;

import figurabia.ui.video.engine.actorframework.MessageSendable;

public class FrameRequest extends ResponseRequest {

    public final long seqNum;
    public final int usageCount;
    public final boolean onlyIfFreeResources;

    public FrameRequest(long seqNum, int usageCount, boolean onlyIfFreeResources, MessageSendable responseTo) {
        super(responseTo);
        this.seqNum = seqNum;
        this.usageCount = usageCount;
        this.onlyIfFreeResources = onlyIfFreeResources;
    }
}
