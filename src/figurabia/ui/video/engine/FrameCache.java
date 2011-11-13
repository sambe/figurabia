/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.10.2010
 */
package figurabia.ui.video.engine;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.messages.CacheBlock;
import figurabia.ui.video.engine.messages.CachedFrame;
import figurabia.ui.video.engine.messages.FetchFrames;
import figurabia.ui.video.engine.messages.FrameRequest;
import figurabia.ui.video.engine.messages.PrefetchRequest;
import figurabia.ui.video.engine.messages.RecyclingBag;
import figurabia.ui.video.engine.messages.CachedFrame.CachedFrameState;

public class FrameCache extends Actor {

    // TODO do timing tests to find the optimal value
    private static final int BLOCK_SIZE = 8;
    private static final int N_CACHE_BLOCKS = 12;
    private static final int CACHE_MIN_RESERVE = 10;
    private static final int CACHE_MAX_RESERVE = 10;

    private final FrameFetcher frameFetcher;

    private final CacheBlock[] cacheBlocks;
    private final Map<Long, CacheBlock> blockByBaseSeqNum = new HashMap<Long, CacheBlock>();
    private final PriorityQueue<CacheBlock> unusedLRU = new PriorityQueue<CacheBlock>();

    private final Queue<FrameRequest> queuedRequests = new LinkedList<FrameRequest>();
    private FrameRequest requestForIdleProcessing = null;

    // actor responsibilities
    // 1) answer requests for frames (by number/?)
    // 2) request frames from FrameFetcher
    // 3) keep frames in a cache with limited size
    // 4) when cache full, start reusing least recently used frame buffers
    // 5) track which buffers are still in use (cannot reload them, until returned)

    public FrameCache(Actor errorHandler, FrameFetcher frameFetcher) {
        super(errorHandler, -1);
        this.frameFetcher = frameFetcher;

        cacheBlocks = new CacheBlock[N_CACHE_BLOCKS];
        for (int i = 0; i < cacheBlocks.length; i++) {
            CachedFrame[] frames = new CachedFrame[BLOCK_SIZE];
            for (int j = 0; j < frames.length; j++) {
                frames[j] = new CachedFrame(i, this);
                frames[j].seqNum = -1;
                frames[j].frame = null;

            }
            cacheBlocks[i] = new CacheBlock(i, frames);
            cacheBlocks[i].baseSeqNum = -1;
            cacheBlocks[i].timestamp = 0L;
            cacheBlocks[i].usageCount = 0;
            cacheBlocks[i].state = CachedFrameState.EMPTY;
        }

        // adding them, so they are available for use
        unusedLRU.addAll(Arrays.asList(cacheBlocks));
    }

    @Override
    protected void act(Object message) {

        // request for frame
        //   - check cache
        //   - if not contains, forward request to frame fetcher (and continue handling)
        if (message instanceof FrameRequest) {
            handleFrameRequest((FrameRequest) message);
        } else if (message instanceof PrefetchRequest) {
            prefetchFrame(((PrefetchRequest) message).baseSeqNum);
        }

        // response from frame fetcher
        //   - mark in cache and send to requester
        else if (message instanceof CacheBlock) {
            CacheBlock block = (CacheBlock) message;
            if (block.usageCount == 0) {
                addUnusedToCache(block);
            } else {
                block.state = CachedFrameState.IN_USE;
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

    @Override
    protected void idle() {

        // if there is a request for idle processing, handle it instead of idling.
        FrameRequest r = requestForIdleProcessing;
        if (r != null && unusedLRU.size() >= CACHE_MAX_RESERVE) {
            requestForIdleProcessing = null;
            handleFrameRequest(new FrameRequest(r.seqNum, r.usageCount, false, r.responseTo));
        } else {
            super.idle();
        }
    }

    private void handleFrameRequest(FrameRequest request) {
        if (request.seqNum < 0) {
            throw new IllegalArgumentException("Request for invalid seq num: " + request.seqNum);
        }
        //DEBUG_cacheCounter.lend(request.seqNum, request.usageCount);

        long seqNumOffset = request.seqNum % BLOCK_SIZE;
        long baseSeqNum = request.seqNum - seqNumOffset;
        CacheBlock block = blockByBaseSeqNum.get(baseSeqNum);
        // if cache contains frame, take it from cache
        if (block != null) {
            if (block.state == CachedFrameState.CACHE) {
                removeFromCache(block);
                block.state = CachedFrameState.IN_USE;
            }
            block.usageCount += request.usageCount;
            if (block.state == CachedFrameState.FETCHING) {
                // already fetching (just queue request for later reply)
                queuedRequests.add(request);
                System.err.println("TRACE: " + request.seqNum + ": 1) already fetching -> request queued");
            } else if (block.state == CachedFrameState.IN_USE) {
                CachedFrame cachedFrame = block.frames[(int) seqNumOffset];
                request.responseTo.send(cachedFrame);
                System.err.println("TRACE: " + request.seqNum + ": 2) cached -> immediate reply");
            } else {
                throw new IllegalStateException("unexpected state of cachedFrame: " + block.state);
            }
        } else { // if cache does not contain frame, request at producer
            if (request.onlyIfFreeResources && unusedLRU.size() < CACHE_MIN_RESERVE) {
                // put on a special waiting slot, where it might be replaced by any later
                requestForIdleProcessing = request;
                System.err.println("TRACE: " + baseSeqNum + ": 3) capacity full -> waiting bench");
            } else {
                // if a onlyIfFreeResources request gets executed again, the previous must be outdated
                if (request.onlyIfFreeResources) {
                    requestForIdleProcessing = null;
                }
                requestFramesFromFetcher(baseSeqNum, request.usageCount);
                // queue request for later answer
                queuedRequests.add(request);
                System.err.println("TRACE: " + baseSeqNum + ": 4) now fetching -> request queued");
            }
        }
    }

    private void requestFramesFromFetcher(long baseSeqNum, int usageCount) {
        CacheBlock block = reuseAndPrepareBlock(baseSeqNum, usageCount);
        frameFetcher.send(new FetchFrames(this, block));
    }

    private void prefetchFrame(long baseSeqNum) {
        if (baseSeqNum % BLOCK_SIZE != 0) {
            throw new IllegalArgumentException("not a base seq num, has offset: " + baseSeqNum);
        }
        if (!blockByBaseSeqNum.containsKey(baseSeqNum)) {
            // usage count is set as zero, because this request just loads something into cache
            requestFramesFromFetcher(baseSeqNum, 0);
        }
    }

    private void processQueuedRequests(Queue<FrameRequest> queuedRequests) {
        while (!queuedRequests.isEmpty()) {
            FrameRequest fr = queuedRequests.peek();
            int seqNumOffset = (int) (fr.seqNum % BLOCK_SIZE);
            long baseSeqNum = fr.seqNum - seqNumOffset;
            CacheBlock block = blockByBaseSeqNum.get(baseSeqNum);
            if (block == null) {
                throw new IllegalStateException("Request in queue for which there is no cache block found: seq num "
                        + fr.seqNum);
            }
            if (block.state == CachedFrameState.CACHE || block.state == CachedFrameState.IN_USE) {
                // send reply
                block.state = CachedFrameState.IN_USE;
                CachedFrame cf = block.frames[seqNumOffset];
                fr.responseTo.send(cf);
                queuedRequests.remove();
            } else {
                // stop if a request cannot be answered yet,
                // to ensure that requests are answered in order
                break;
            }
        }
    }

    private CacheBlock reuseAndPrepareBlock(long baseSeqNum, int usageCount) {
        CacheBlock block = getUnusedFromCache();
        if (block.usageCount != 0) {
            throw new IllegalStateException("Cache block must be unused right now (usageCount = " + block.usageCount
                    + ")");
        }
        block.state = CachedFrameState.FETCHING;
        blockByBaseSeqNum.remove(block.baseSeqNum); // no longer representing the old baseSeqNum
        block.baseSeqNum = baseSeqNum;
        blockByBaseSeqNum.put(baseSeqNum, block);
        block.usageCount += usageCount;
        return block;
    }

    private void recycleFrame(CachedFrame cf) {
        CacheBlock block = cacheBlocks[cf.index];
        if (block.usageCount == 0) {
            System.err.println("WARN: received a frame for recycling that whose block was already fully returned for recycling. "
                    + cf.seqNum + " at block index " + block.index);
            return;
        }
        block.usageCount--;
        if (block.usageCount < 0) {
            System.err.println("ILLEGAL_STATE: block.usageCount is negative (" + block.usageCount + ") for block "
                    + block.index);
        }
        if (block.usageCount == 0) {
            addUnusedToCache(block);
        }
        //DEBUG_cacheCounter.giveBack(cf.seqNum);
    }

    private void addUnusedToCache(CacheBlock block) {
        if (block.usageCount != 0) {
            throw new IllegalStateException("Tried adding a cache block to the cache that is in use: cache index "
                    + block.index
                    + ", seq num " + block.baseSeqNum + ", state " + block.state);
        }
        block.state = CachedFrameState.CACHE;
        block.timestamp = System.currentTimeMillis();
        unusedLRU.add(block);
        //System.err.println("DEBUG: Returned block to unused blocks: " + unusedLRU.size() + " (usage count: "
        //        + block.usageCount + ") index: " + block.index);
    }

    private void removeFromCache(CacheBlock block) {
        unusedLRU.remove(block);
    }

    private CacheBlock getUnusedFromCache() {
        System.err.println("DEBUG: Unused cache blocks available: " + unusedLRU.size());
        if (unusedLRU.isEmpty()) {
            //DEBUG_cacheCounter.printStatistics();
            throw new IllegalStateException("No cache block available to reuse");
        }
        CacheBlock block = unusedLRU.poll();
        //System.err.println("DEBUG: removed cache block " + block.index + ", usageCount: " + block.usageCount);
        return block;
    }

    /*private CacheCounter DEBUG_cacheCounter = new CacheCounter();

    private class CacheCounter {
        private Map<Long, Integer> cacheCounter = new TreeMap<Long, Integer>();

        public void lend(Long seqNum, int count) {
            Integer prevCount = cacheCounter.get(seqNum);
            if (prevCount == null) {
                prevCount = 0;
            }
            cacheCounter.put(seqNum, prevCount + count);
        }

        public void giveBack(Long seqNum) {
            Integer prevCount = cacheCounter.get(seqNum);
            if (prevCount == null) {
                System.err.println("FAILED ASSERTION: returned seqNum " + seqNum + ", but was not registered");
                prevCount = 0;
            }
            int newCount = prevCount - 1;
            if (newCount < 0)
                System.err.println("FAILED ASSERTION: return seqNum " + seqNum + " once too often");
            cacheCounter.put(seqNum, newCount);
        }

        public void printStatistics() {
            for (Entry<Long, Integer> e : cacheCounter.entrySet()) {
                if (e.getValue() != 0) {
                    System.err.println("STAT: seqNum: " + e.getKey() + "; count: " + e.getValue());
                }
            }
        }
    }*/
}
