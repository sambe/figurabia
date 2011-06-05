/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 25.04.2011
 */
package figurabia.ui.video.engine;

import java.util.LinkedList;
import java.util.Queue;

import javax.media.format.VideoFormat;

import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.actorframework.ObjectReceiver;
import figurabia.ui.video.engine.messages.AudioSyncEvent;
import figurabia.ui.video.engine.messages.CachedFrame;
import figurabia.ui.video.engine.messages.ControlCommand;
import figurabia.ui.video.engine.messages.FrameRequest;
import figurabia.ui.video.engine.messages.MediaInfoRequest;
import figurabia.ui.video.engine.messages.MediaInfoResponse;
import figurabia.ui.video.engine.messages.NewVideo;
import figurabia.ui.video.engine.messages.RecyclingBag;
import figurabia.ui.video.engine.messages.SetPosition;
import figurabia.ui.video.engine.messages.ControlCommand.Command;

public class Controller extends Actor {

    private static final int PREFETCH_SIZE = 5;
    private static final int USAGE_COUNT = 2;
    private static final int SYNC_OFFSET = 250;

    private static enum State {
        STOPPED,
        PREPARING,
        PLAYING;
    }

    private Actor errorHandler;
    private FrameFetcher frameFetcher;
    private FrameCache frameCache;
    private AudioRenderer audioRenderer;
    private final VideoRenderer videoRenderer;

    private VideoFormat videoFormat;

    private Queue<CachedFrame> queuedFrames = new LinkedList<CachedFrame>();
    private Timer timer = new Timer();

    private long nextSeqNumExpected = -1;
    private long startingTimerPos = 0;

    private State state;

    public Controller(Actor errorHandler) {
        super(errorHandler);
        this.errorHandler = errorHandler;

        videoRenderer = new VideoRenderer(errorHandler);
        videoRenderer.start();
    }

    @Override
    protected void act(Object message) {
        if (message instanceof NewVideo) {
            loadVideo((NewVideo) message);
        } else if (message instanceof ControlCommand) {
            handleControlCommand((ControlCommand) message);
        } else if (message instanceof SetPosition) {
            handleSetPosition((SetPosition) message);
        } else if (message instanceof CachedFrame) {
            handleCachedFrame((CachedFrame) message);
        } else if (message instanceof AudioSyncEvent) {
            handleAudioSyncEvent((AudioSyncEvent) message);
        } else {
            throw new IllegalStateException("unknown type of message: " + message.getClass());
        }
    }

    private void loadVideo(NewVideo message) {
        if (frameFetcher != null) {
            nextSeqNumExpected = -1;
            clearQueuedFrames();

            frameFetcher.stop();
            frameCache.stop();

            audioRenderer.send(new ControlCommand(Command.STOP));
            audioRenderer.send(new ControlCommand(Command.FLUSH));
            timer.stop();

            audioRenderer.start();
        }

        frameFetcher = new FrameFetcher(errorHandler, message.videoFile);
        frameCache = new FrameCache(errorHandler, frameFetcher);

        frameFetcher.start();
        frameCache.start();

        ObjectReceiver receiver = new ObjectReceiver();
        frameFetcher.send(new MediaInfoRequest(receiver));
        final MediaInfoResponse mir = (MediaInfoResponse) receiver.waitForMessage();

        audioRenderer = new AudioRenderer(errorHandler, mir.audioFormat, this);
        audioRenderer.start();
        videoFormat = mir.videoFormat;

        state = State.STOPPED;
        timer.setPosition(message.initialPosition);
    }

    private void handleAudioSyncEvent(AudioSyncEvent message) {
        switch (message.type) {
        case START:
            state = State.PLAYING;
            timer.setPosition(startingTimerPos + SYNC_OFFSET);
            break;
        case STOP:
            break;
        default:
            throw new IllegalStateException("unknown audio sync event: " + message.type);
        }
    }

    private void handleControlCommand(ControlCommand message) {
        switch (message.command) {
        case START:
            // TODO maybe has to wait until frames are available (cachedFrames non empty)
            if (!timer.isRunning()) {
                state = State.PREPARING;
                startingTimerPos = timer.getPosition();
                long currentSeqNum = calculateSeqNum(startingTimerPos);
                if (nextSeqNumExpected != currentSeqNum) {
                    prefetch(currentSeqNum);
                }
                audioRenderer.send(message);
                timer.start();
                // timer will be repositioned in handleAudioSyncEvent
            }
            break;
        case STOP:
            if (timer.isRunning()) {
                state = State.STOPPED;
                timer.stop();
                audioRenderer.send(message);
            }
            break;
        default:
            throw new IllegalArgumentException("controller only supports start and stop");
        }
    }

    private void handleSetPosition(SetPosition message) {
        long positionSeqNum = calculateSeqNum(message.position);

        if (timer.isRunning()) {
            // first flush everything
            audioRenderer.send(new ControlCommand(Command.FLUSH));
            clearQueuedFrames();

            // send fetching requests
            prefetch(positionSeqNum);
        } else {
            sendFetchRequest(positionSeqNum);
            clearQueuedFrames();
        }

        // move timer
        timer.setPosition(positionSeqNum);
    }

    private void handleCachedFrame(CachedFrame frame) {
        if (state == State.PREPARING || state == State.PLAYING) {
            if (frame.seqNum != nextSeqNumExpected) {
                // silently drop (was too late, no longer needed)
                recycle(frame, USAGE_COUNT);
                return;
            }
            nextSeqNumExpected++;
            queuedFrames.add(frame);
            audioRenderer.send(frame);
        } else {
            videoRenderer.send(frame);
        }
    }

    private void prefetch(long positionSeqNum) {
        nextSeqNumExpected = positionSeqNum;
        for (int i = 0; i < PREFETCH_SIZE; i++) {
            sendFetchRequest(positionSeqNum + i);
        }
    }

    private void sendFetchRequest(long seqNum) {
        frameCache.send(new FrameRequest(seqNum, USAGE_COUNT, this));
    }

    private void clearQueuedFrames() {
        for (CachedFrame cf : queuedFrames) {
            recycle(cf, 1);
        }
        queuedFrames.clear();
    }

    private void recycle(CachedFrame cf, int times) {
        for (int i = 0; i < times; i++) {
            frameCache.send(new RecyclingBag(cf));
        }
    }

    @Override
    protected void idle() {
        super.idle();

        // simpler first running version: no synchronization, just 5 frames ahead (the first 5 on setting position)
        // prefetching TODO (+ initial prefetching on position request & initialization, -> delayed start after the minimum has arrived, sent to sound and sound responded it started)

        // rendering (TODO sound, probably immediately pass on)
        if (timer.isRunning() && !queuedFrames.isEmpty()) {
            // paint current frame if it changed
            long currentSeqNum = calculateSeqNum(timer.getPosition());
            long frameSeqNum = queuedFrames.peek().seqNum;
            //System.out.println("currentSeqNum = " + currentSeqNum + "; frameSeqNum = " + frameSeqNum);
            if (frameSeqNum <= currentSeqNum) {
                videoRenderer.send(queuedFrames.poll());
                sendFetchRequest(frameSeqNum + PREFETCH_SIZE);
            }
        }
    }

    private long calculateSeqNum(long position) {
        return (long) Math.floor(position * videoFormat.getFrameRate() / 1000);
    }

    /**
     * This timer knows two states: running or not running. If it is running the current position is based on
     * <code>zeroPoint</code> which marks the point in time when playback started (if it would not have ever been
     * interrupted inbetween). If it is not running the current position is stored in <code>pos</code>.
     */
    private static class Timer {
        private boolean running = false;
        private long zeroPoint = -1;
        private long pos = 0;

        private long getPositionWhileRunning() {
            long now = System.currentTimeMillis();
            return now - zeroPoint;
        }

        private long getZeroPoint(long pos) {
            long now = System.currentTimeMillis();
            return now - pos;
        }

        public void start() {
            if (!running) {
                running = true;
                zeroPoint = getZeroPoint(pos);
                pos = -1;
            }
        }

        public void stop() {
            if (running) {
                running = false;
                pos = getPositionWhileRunning();
                zeroPoint = -1;
            }
        }

        public long getPosition() {
            if (running) {
                return getPositionWhileRunning();
            } else {
                return pos;
            }
        }

        public void setPosition(long newPos) {
            if (running) {
                zeroPoint = getZeroPoint(newPos);
            } else {
                pos = newPos;
            }
        }

        public boolean isRunning() {
            return running;
        }
    }

    /**
     * @return the videoRenderer
     */
    public VideoRenderer getVideoRenderer() {
        return videoRenderer;
    }
}
