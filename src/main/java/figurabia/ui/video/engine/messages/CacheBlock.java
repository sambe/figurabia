/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.08.2011
 */
package figurabia.ui.video.engine.messages;

import figurabia.ui.video.engine.messages.CachedFrame.CachedFrameState;

public class CacheBlock implements Comparable<CacheBlock> {

    public CacheBlock(int index, CachedFrame[] frames) {
        this.index = index;
        this.frames = frames;
    }

    public final int index;
    public final CachedFrame[] frames;

    public long baseSeqNum;
    public long timestamp;
    public int usageCount;
    public CachedFrameState state;

    @Override
    public int compareTo(CacheBlock o) {
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
