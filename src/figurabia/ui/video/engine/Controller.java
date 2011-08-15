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
import figurabia.ui.video.engine.messages.PositionUpdate;
import figurabia.ui.video.engine.messages.RecyclingBag;
import figurabia.ui.video.engine.messages.SetPosition;
import figurabia.ui.video.engine.messages.SetSpeed;
import figurabia.ui.video.engine.messages.StateUpdate;
import figurabia.ui.video.engine.messages.ControlCommand.Command;

public class Controller extends Actor {

    private static final int PREFETCH_SIZE = 5;
    private static final int USAGE_COUNT = 2;
    private static final int SYNC_OFFSET = 50; //250;
    private static final double MIN_VALID_SPEED = 1.0 / 40.0;

    public static enum State {
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
    private SetPosition anotherPosSetDuringPreparing = null;

    private long timerMin = -1;
    private long timerMax = -1;
    private long endFrameSeqNum = -1;

    private boolean looping = false;

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
        } else if (message instanceof SetSpeed) {
            handleSetSpeed((SetSpeed) message);
        } else if (message instanceof CachedFrame) {
            //System.out.println("Controller receiving frame " + ((CachedFrame) message).seqNum);
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

        if (message.positionMin != null) {
            timerMin = message.positionMin;
        } else {
            timerMin = 0;
        }
        if (message.positionMax != null) {
            timerMax = message.positionMax;
        } else {
            timerMax = Math.round(mir.duration * 1000.0);
        }
        endFrameSeqNum = calculateSeqNum(timerMax);

        setState(State.STOPPED);
        setPosition(message.initialPosition);
    }

    private void handleAudioSyncEvent(AudioSyncEvent message) {
        switch (message.type) {
        case START:
            setState(State.PLAYING);
            if (anotherPosSetDuringPreparing != null) {
                // handle SetPosition that was queued, instead of syncing (will change anyway)
                SetPosition messageToSend = anotherPosSetDuringPreparing;
                anotherPosSetDuringPreparing = null;
                handleSetPosition(messageToSend);
            } else {
                // sync video to sound
                timer.setPosition(startingTimerPos + SYNC_OFFSET);
            }
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
                setState(State.PREPARING);
                startingTimerPos = timer.getPosition();
                long currentSeqNum = calculateSeqNum(startingTimerPos);
                // if at end: start playing from beginning
                if (currentSeqNum >= endFrameSeqNum) {
                    timer.setPosition(timerMin);
                    startingTimerPos = timerMin;
                    currentSeqNum = calculateSeqNum(timerMin);
                }
                prefetch(currentSeqNum);
                audioRenderer.send(message);
                timer.start();
                // timer will be repositioned in handleAudioSyncEvent
            }
            break;
        case STOP:
            if (timer.isRunning()) {
                setState(State.STOPPED);
                timer.stop();
                audioRenderer.send(message);
                audioRenderer.send(new ControlCommand(Command.FLUSH));
            }
            break;
        default:
            throw new IllegalArgumentException("controller only supports start and stop");
        }
    }

    private void handleSetPosition(SetPosition message) {
        startingTimerPos = message.position;
        long positionSeqNum = calculateSeqNum(startingTimerPos);
        //System.out.println("positionSeqNum: " + positionSeqNum + " derived from " + message.position);

        if (timer.isRunning()) {
            if (state == State.PREPARING) {
                // if already in PREPARING: remember for later to avoid running out of buffers (due to overload)
                anotherPosSetDuringPreparing = message;
            } else {
                setState(State.PREPARING);
                // first flush everything
                audioRenderer.send(new ControlCommand(Command.STOP));
                audioRenderer.send(new ControlCommand(Command.FLUSH));
                clearQueuedFrames();

                // send fetching requests
                prefetch(positionSeqNum);
                audioRenderer.send(new ControlCommand(Command.START));
            }
        } else {
            sendFetchRequest(positionSeqNum, true);
            clearQueuedFrames();
        }

        // move timer
        setPosition(startingTimerPos);
    }

    private void handleSetSpeed(SetSpeed message) {
        if (Math.abs(message.newSpeed) < MIN_VALID_SPEED) {
            // ignore, because this is not a valid speed
            return;
        }

        boolean running = timer.isRunning();

        if (running) {
            setState(State.PREPARING);
            // first flush everything
            audioRenderer.send(new ControlCommand(Command.STOP));
            audioRenderer.send(new ControlCommand(Command.FLUSH));
            clearQueuedFrames();
        }

        startingTimerPos = timer.getPosition();
        timer.setSpeed(message.newSpeed);
        audioRenderer.send(message);

        if (running) {
            long currentTime = timer.getPosition();
            long currentSeqNum = calculateSeqNum(currentTime);
            // send fetching requests
            prefetch(currentSeqNum);
            audioRenderer.send(new ControlCommand(Command.START));
        }
    }

    private void handleCachedFrame(CachedFrame frame) {
        System.out.println("TRACE: " + frame.seqNum + ": received in state " + frame.state);
        if (state == State.PREPARING || state == State.PLAYING) {
            if (frame.seqNum != nextSeqNumExpected) {
                System.out.println("DEBUG: dropping frame " + frame.seqNum + " because expected " + nextSeqNumExpected);
                // silently drop (was too late, no longer needed)
                recycle(frame, USAGE_COUNT);
                return;
            }
            nextSeqNumExpected++;
            System.out.println("TRACE: " + frame.seqNum + ": added to queued frames");
            queuedFrames.add(frame);
            audioRenderer.send(frame);
        } else { // result of moving position in stopped state -> immediately display
            // already recycle once because they're not sent to audio renderer
            frame.recycle();
            videoRenderer.send(frame);
        }
    }

    private void setState(State state) {
        if (this.state != state) {
            this.state = state;
            sendUpdate(new StateUpdate(state));
        }
    }

    private void setPosition(long newPosition) {
        timer.setPosition(newPosition);
        sendUpdate(new PositionUpdate(newPosition, timerMin, timerMax));
    }

    private void prefetch(long positionSeqNum) {
        System.out.println("DEBUG: " + positionSeqNum + ": prefetching");
        nextSeqNumExpected = positionSeqNum;
        for (int i = 0; i < PREFETCH_SIZE; i++) {
            sendFetchRequest(positionSeqNum + i, false);
        }
    }

    private void sendFetchRequest(long seqNum, boolean onlyIfFreeResources) {
        frameCache.send(new FrameRequest(seqNum, USAGE_COUNT, onlyIfFreeResources, this));
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
        if (timer.isRunning()) {
            // paint current frame if it changed
            long currentTime = timer.getPosition();
            long currentSeqNum = calculateSeqNum(currentTime);
            if (queuedFrames.isEmpty()) {
                if (currentSeqNum >= endFrameSeqNum) {
                    if (looping) {
                        handleSetPosition(new SetPosition(timerMin));
                    } else {
                        handleControlCommand(new ControlCommand(Command.STOP));
                    }
                }
            } else {
                long frameSeqNum = queuedFrames.peek().seqNum;
                System.out.println("TRACE: currentSeqNum = " + currentSeqNum + " (based on current time = "
                        + currentTime
                        + "); queuedFrames.peek().seqNum = " + frameSeqNum);
                if (frameSeqNum <= currentSeqNum) {
                    videoRenderer.send(queuedFrames.poll());
                    if (frameSeqNum + PREFETCH_SIZE < endFrameSeqNum) {
                        sendFetchRequest(frameSeqNum + PREFETCH_SIZE, false);
                    }
                    sendUpdate(new PositionUpdate(currentTime, timerMin, timerMax));
                    System.out.println("TRACE: " + (frameSeqNum + PREFETCH_SIZE) + ": sent fetch request");
                }
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
        private double speed = 1.0;

        private long getPositionWhileRunning() {
            long now = System.currentTimeMillis();
            return Math.round((now - zeroPoint) * speed);
        }

        private long getZeroPoint(long pos) {
            long now = System.currentTimeMillis();
            return now - Math.round(pos / speed);
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

        public double getSpeed() {
            return speed;
        }

        public void setSpeed(double newSpeed) {
            if (running) {
                long position = getPositionWhileRunning();
                speed = newSpeed;
                zeroPoint = getZeroPoint(position);
            } else {
                speed = newSpeed;
            }
        }
    }

    /**
     * @return the videoRenderer
     */
    public VideoRenderer getVideoRenderer() {
        return videoRenderer;
    }
}
