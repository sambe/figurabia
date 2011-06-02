/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.actorframework.MessageSendable;
import figurabia.ui.video.engine.messages.CachedFrame;
import figurabia.ui.video.engine.messages.FetchFrames;
import figurabia.ui.video.engine.messages.FrameRequest;
import figurabia.ui.video.engine.messages.PrefetchRequest;
import figurabia.ui.video.engine.messages.RecyclingBag;
import figurabia.ui.video.engine.messages.CachedFrame.CachedFrameState;

public class FrameCache extends Actor {

    private static enum RetrievalMode {
        STREAM,
        SINGLE_FRAMES;
    }

    // TODO do timing tests to find the optimal value
    private static final int BATCH_SIZE = 8;
    private static final int CACHE_SIZE = 96;

    private final FrameFetcher frameFetcher;

    private final CachedFrame[] frames;
    private final Map<Long, CachedFrame> framesBySeqNum = new HashMap<Long, CachedFrame>();
    private final PriorityQueue<CachedFrame> unusedLRU = new PriorityQueue<CachedFrame>();

    private final Queue<FrameRequest> queuedRequests = new LinkedList<FrameRequest>();

    private RetrievalMode retrievalMode = RetrievalMode.STREAM;

    // actor responsibilities
    // 1) answer requests for frames (by number/?)
    // 2) request frames from FrameFetcher
    // 3) keep frames in a cache with limited size
    // 4) when cache full, start reusing least recently used frame buffers
    // 5) track which buffers are still in use (cannot reload them, until returned)

    public FrameCache(Actor errorHandler, FrameFetcher frameFetcher) {
        super(errorHandler);
        this.frameFetcher = frameFetcher;

        frames = new CachedFrame[CACHE_SIZE];
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new CachedFrame(i, this);
            frames[i].seqNum = -1;
            frames[i].timestamp = 0;
            frames[i].frame = null;
            frames[i].usageCount = 0;
        }

        // adding them, so they are available for use
        unusedLRU.addAll(Arrays.asList(frames));
    }

    @Override
    protected void act(Object message) {

        // request for frame
        //   - check cache
        //   - if not contains, forward request to frame fetcher (and continue handling)
        if (message instanceof FrameRequest) {
            handleFrameRequest((FrameRequest) message);
        } else if (message instanceof PrefetchRequest) {
            handlePrefetchRequest((PrefetchRequest) message);
        }

        // response from frame fetcher
        //   - mark in cache and send to requester
        else if (message instanceof FetchFrames) {
            FetchFrames ff = (FetchFrames) message;
            for (CachedFrame cf : ff.frames) {
                addUnusedToCache(cf);
            }
            processQueuedRequests(queuedRequests);
        }

        // recycling bag
        //   - mark frame from bag as no longer in use (reusable for new requests, but still containing a buffered frame)
        else if (message instanceof RecyclingBag) {
            RecyclingBag rb = (RecyclingBag) message;
            recycleFrame((CachedFrame) rb.object);
        }
    }

    private void handleFrameRequest(FrameRequest request) {
        CachedFrame cachedFrame = framesBySeqNum.get(request.seqNum);
        // if cache contains frame, take it from cache
        if (cachedFrame != null) {
            if (cachedFrame.state == CachedFrameState.FETCHING) {
                // already fetching (just queue request for later reply)
                queuedRequests.add(request);
            } else { // cachedFrame.state is either CACHE or IN_USE (or EMPTY)
                replyFrameRequest(cachedFrame, request.usageCount, request.responseTo);
            }
        } else { // if cache does not contain frame, request at producer
            requestFrameFromFetcher(request.seqNum);
            // queue request for later answer
            queuedRequests.add(request);
        }
    }

    private void requestFrameFromFetcher(long seqNum) {
        if (retrievalMode == RetrievalMode.SINGLE_FRAMES) {
            CachedFrame cf = reuseAndPrepareFrame(seqNum);
            frameFetcher.send(new FetchFrames(this, seqNum, Arrays.asList(cf)));
        } else if (retrievalMode == RetrievalMode.STREAM) {
            List<CachedFrame> frameBatch = new ArrayList<CachedFrame>();
            long baseSeqNum = seqNum / BATCH_SIZE * BATCH_SIZE;
            for (int i = 0; i < BATCH_SIZE; i++) {
                CachedFrame cf = reuseAndPrepareFrame(baseSeqNum + i);
                frameBatch.add(cf);
            }
            frameFetcher.send(new FetchFrames(this, baseSeqNum, frameBatch));
        }
    }

    private void prefetchFrame(long seqNum) {
        if (!framesBySeqNum.containsKey(seqNum)) {
            requestFrameFromFetcher(seqNum);
        }
    }

    private void handlePrefetchRequest(PrefetchRequest request) {
        if (request.startSeqNum > request.endSeqNum) {
            throw new IllegalArgumentException("start seq before end seq: " + request.startSeqNum + " "
                    + request.endSeqNum);
        }
        for (long seqNum = request.startSeqNum; seqNum < request.endSeqNum; seqNum++) {
            prefetchFrame(seqNum);
        }
    }

    private void replyFrameRequest(CachedFrame cachedFrame, int usageCountToAdd, MessageSendable responseTo) {
        if (cachedFrame.usageCount == 0) {
            unusedLRU.remove(cachedFrame);
            cachedFrame.state = CachedFrameState.IN_USE;
        }
        cachedFrame.usageCount += usageCountToAdd;
        responseTo.send(cachedFrame);
    }

    private void processQueuedRequests(Queue<FrameRequest> queuedRequests) {
        while (!queuedRequests.isEmpty()) {
            FrameRequest fr = queuedRequests.peek();
            CachedFrame cf = framesBySeqNum.get(fr.seqNum);
            if (cf == null) {
                throw new IllegalStateException("Request in queue for which there is no CachedFrame waiting: seq num "
                        + fr.seqNum);
            }
            if (cf.state == CachedFrameState.CACHE || cf.state == CachedFrameState.IN_USE) {
                replyFrameRequest(cf, fr.usageCount, fr.responseTo);
                queuedRequests.remove();
            } else {
                // stop if a request cannot be answered yet,
                // to ensure that requests are answered in order
                break;
            }
        }
    }

    private CachedFrame reuseAndPrepareFrame(long seqNum) {
        CachedFrame cf = getUnusedFromCache();
        cf.state = CachedFrameState.FETCHING;
        framesBySeqNum.remove(cf.seqNum); // no longer representing the old seqNum
        cf.seqNum = seqNum;
        framesBySeqNum.put(cf.seqNum, cf);
        return cf;
    }

    private void recycleFrame(CachedFrame cf) {
        if (cf.usageCount == 0) {
            System.err.println("WARN: received a frame for recycling that was already fully returned for recycling. "
                    + cf.seqNum + " at cache index " + cf.index);
            return;
        }
        cf.usageCount--;
        if (cf.usageCount == 0) {
            addUnusedToCache(cf);
        }
    }

    private void addUnusedToCache(CachedFrame cf) {
        if (cf.usageCount != 0) {
            throw new IllegalStateException("Tried adding a frame to the cache that is in use: cache index " + cf.index
                    + ", seq num " + cf.seqNum + ", state " + cf.state);
        }
        cf.state = CachedFrameState.CACHE;
        unusedLRU.add(cf);
    }

    private CachedFrame getUnusedFromCache() {
        //System.out.println("DEBUG: Unused cache frames available: " + unusedLRU.size());
        if (unusedLRU.isEmpty()) {
            throw new IllegalStateException("No frame available to reuse");
        }
        return unusedLRU.poll();
    }
}
