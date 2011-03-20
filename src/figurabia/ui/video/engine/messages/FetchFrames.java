/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine.messages;

import java.util.List;

import figurabia.ui.video.engine.actorframework.MessageSendable;

public class FetchFrames extends ResponseRequest {

    public final long startSeqNr;
    public final List<CachedFrame> frames;

    public FetchFrames(MessageSendable responseTo, long startSeqNr, List<CachedFrame> frames) {
        super(responseTo);
        this.startSeqNr = startSeqNr;
        this.frames = frames;
    }

}
