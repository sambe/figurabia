/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on May 20, 2012
 */
package figurabia.ui.video.access;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MediaInfo {

    public final List<Long> keyFrameTimestamps;
    public final double videoPacketTimeBase;
    public final double audioPacketTimeBase;
    public final double pictureTimeBase;
    public final double samplesTimeBase;
    public final int audioFrameSize;
    public final double videoFrameRate;

    public MediaInfo(List<Long> keyFrameTimestamps, double videoPacketTimeBase, double audioPacketTimeBase,
            double pictureTimeBase, double samplesTimeBase, int audioFrameSize, double videoFrameRate) {
        this.keyFrameTimestamps = Collections.unmodifiableList(new ArrayList<Long>(keyFrameTimestamps));
        this.videoPacketTimeBase = videoPacketTimeBase;
        this.audioPacketTimeBase = audioPacketTimeBase;
        this.pictureTimeBase = pictureTimeBase;
        this.samplesTimeBase = samplesTimeBase;
        this.audioFrameSize = audioFrameSize;
        this.videoFrameRate = videoFrameRate;
    }

    public long findRelevantKeyframeTimestamp(long targetValue) {
        if (targetValue < 0)
            return 0;
        int low = 0;
        int high = keyFrameTimestamps.size() - 1;
        if (high == -1)
            return targetValue; // just return value itself as a fallback
        while (low < high) {
            int median = low + (high - low + 1) / 2;
            long valueAtMedian = keyFrameTimestamps.get(median);
            if (valueAtMedian > targetValue)
                high = median - 1;
            else if (valueAtMedian < targetValue)
                low = median;
            else
                return targetValue; // exact match
        }
        return keyFrameTimestamps.get(low);
    }
}
