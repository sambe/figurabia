/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 01.01.2011
 */
package figurabia.ui.video.engine.messages;

import figurabia.ui.video.access.MediaFrame;

public class CachedFrame implements Comparable<CachedFrame> {

    public enum CachedFrameState {
        /**
         * When no yet initialized (does not yet contain a buffer)
         */
        EMPTY,
        /**
         * When fetching (cannot reuse, because already in fetching process)
         */
        FETCHING,
        /**
         * When cached (can reuse)
         */
        CACHE,
        /**
         * When sent as a response to a request and not yet returned (cannot reuse until returned)
         */
        IN_USE;
    }

    public int index;
    public long seqNum;
    public MediaFrame frame;
    public long timestamp;
    public int usageCount;
    public CachedFrameState state = CachedFrameState.EMPTY;

    @Override
    public int compareTo(CachedFrame o) {
        long diff = timestamp - o.timestamp;
        if (diff < 0) {
            return -1;
        } else if (diff > 0) {
            return 1;
        } else {
            return 0;
        }
    }
}
