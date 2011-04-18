/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 10.04.2011
 */
package figurabia.ui.video.engine.messages;

public class PrefetchRequest {
    public final long startSeqNum;
    public final long endSeqNum;

    public PrefetchRequest(long startSeqNum, long endSeqNum) {
        super();
        this.startSeqNum = startSeqNum;
        this.endSeqNum = endSeqNum;
    }
}
