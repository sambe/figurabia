/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on May 20, 2012
 */
package figurabia.ui.video.access;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class MediaInfoTest {

    @Test
    public void testFindRelevantKeyframeTimestamp() {

        List<Long> timestamps = Arrays.asList(0L, 800000L, 1600000L, 2400000L);
        MediaInfo mi = new MediaInfo(timestamps, 0, 0, 0, 0, 0);

        Assert.assertEquals(0L, mi.findRelevantKeyframeTimestamp(-500000));
        Assert.assertEquals(0L, mi.findRelevantKeyframeTimestamp(0));
        Assert.assertEquals(0L, mi.findRelevantKeyframeTimestamp(200000));
        Assert.assertEquals(800000L, mi.findRelevantKeyframeTimestamp(800000));
        Assert.assertEquals(800000L, mi.findRelevantKeyframeTimestamp(1000000));
        Assert.assertEquals(800000L, mi.findRelevantKeyframeTimestamp(1500000));
        Assert.assertEquals(1600000, mi.findRelevantKeyframeTimestamp(1600000));
        Assert.assertEquals(1600000, mi.findRelevantKeyframeTimestamp(1650000));
        Assert.assertEquals(2400000, mi.findRelevantKeyframeTimestamp(2400000));
        Assert.assertEquals(2400000, mi.findRelevantKeyframeTimestamp(3000000));
    }
}
