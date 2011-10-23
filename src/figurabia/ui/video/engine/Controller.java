/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 25.04.2011
 */
package figurabia.ui.video.engine;

import java.util.Comparator;
import java.util.PriorityQueue;

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
import figurabia.ui.video.engine.messages.SetPosition;
import figurabia.ui.video.engine.messages.SetSpeed;
import figurabia.ui.video.engine.messages.StateUpdate;
import figurabia.ui.video.engine.messages.StatusRequest;
import figurabia.ui.video.engine.messages.StatusResponse;
import figurabia.ui.video.engine.messages.ControlCommand.Command;

public class Controller extends Actor {

    private static final int DEFAULT_PREFETCH_SIZE = 5;
    private static final int MAX_EXPECTED_PREFETCH_SIZE = DEFAULT_PREFETCH_SIZE * 4;
    private static final int USAGE_COUNT = 2;
    private static final int SYNC_OFFSET = 50; //250;
    private static final double MIN_VALID_SPEED = 1.0 / 40.0;
    private static final double ANIMATION_SPEED = 2.0;

    public static enum State {
        STOPPED(false),
        PREPARING(true),
        PLAYING(true);

        public final boolean running;

        private State(boolean running) {
            this.running = running;
        }
    }

    private Actor errorHandler;
    private FrameFetcher frameFetcher;
    private FrameCache frameCache;
    private AudioRenderer audioRenderer;
    private final VideoRenderer videoRenderer;

    // neutral parts (only depending on video)
    private VideoFormat videoFormat;
    private long duration = -1;

    private Engine engine = new Engine();

    // TODO this block should go into the PlayConstraints
    private class PlayConstraints {
        boolean running = false;
        long timerMin = -1;
        long timerMax = -1;
        long startFrameSeqNum = -1; // dependent on timerMin
        long endFrameSeqNum = -1; // dependent on timerMax
        double playerSpeed = 1.0;
        int prefetchSize = DEFAULT_PREFETCH_SIZE; // dependent on playerSpeed
        boolean looping = false;
        boolean mute = false;
        boolean positionUpdates = true;

        void setTimerMin(Long value) {
            if (value == null) {
                timerMin = 0;
            } else {
                timerMin = value;
            }
            startFrameSeqNum = calculateSeqNum(timerMin);
        }

        void setTimerMax(Long value) {
            if (value == null) {
                timerMax = duration;
            } else {
                timerMax = value;
            }
            endFrameSeqNum = calculateSeqNum(timerMax);
        }

        void setSpeed(double speed) {
            if (Math.abs(speed) < MIN_VALID_SPEED) {
                // ignore, because this is not a valid speed
                return;
            }
            playerSpeed = speed;
            double absSpeed = Math.abs(playerSpeed);
            if (absSpeed > 1.0) {
                prefetchSize = (int) (DEFAULT_PREFETCH_SIZE * absSpeed);
            } else {
                prefetchSize = DEFAULT_PREFETCH_SIZE;
            }
        }

        void copyFrom(PlayConstraints pc) {
            running = pc.running;
            timerMin = pc.timerMin;
            timerMax = pc.timerMax;
            startFrameSeqNum = pc.startFrameSeqNum;
            endFrameSeqNum = pc.endFrameSeqNum;
            playerSpeed = pc.playerSpeed;
            prefetchSize = pc.prefetchSize;
            positionUpdates = pc.positionUpdates;
            looping = pc.looping;
            mute = pc.mute;
        }
    }

    private PlayConstraints controlCons = new PlayConstraints();

    private State state;

    public Controller(Actor errorHandler) {
        super(errorHandler, 10);
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
            engine.handleCachedFrame((CachedFrame) message);
        } else if (message instanceof AudioSyncEvent) {
            handleAudioSyncEvent((AudioSyncEvent) message);
        } else if (message instanceof StatusRequest) {
            handleStatusRequest((StatusRequest) message);
        } else if (message instanceof MediaInfoRequest) {
            // has to be here, otherwise it would be handled by the old frame fetcher
            frameFetcher.send(message);
        } else if (message instanceof FrameRequest) {
            frameCache.send(message);
        } else {
            throw new IllegalStateException("unknown type of message: " + message.getClass());
        }
    }

    private void loadVideo(NewVideo message) {
        if (frameFetcher != null) {
            engine.reset();

            frameFetcher.stop();
            frameCache.stop();

            engine.stop();

            audioRenderer.stop();
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

        duration = mir.duration;
        controlCons.setTimerMin(message.positionMin);
        controlCons.setTimerMax(message.positionMax);
        engine.setPlayConstraints(controlCons, null);

        setState(State.STOPPED);
        long timerInitial;
        if (message.initialPosition != null) {
            timerInitial = message.initialPosition;
        } else {
            timerInitial = controlCons.timerMin;
        }
        engine.setPosition(timerInitial);
    }

    private void handleAudioSyncEvent(AudioSyncEvent message) {
        switch (message.type) {
        case START:
            engine.synchronize();
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
            // TODO regard controlCons.running, separate between inner (engine) and outer (control)
            if (!controlCons.running) {
                controlCons.running = true;
                setState(State.PLAYING);
            }
            engine.setPlayConstraints(controlCons, null);
            break;
        case STOP:
            if (controlCons.running) {
                controlCons.running = false;
                setState(State.STOPPED);
            }
            engine.setPlayConstraints(controlCons, null);
            break;
        default:
            throw new IllegalArgumentException("controller only supports start and stop");
        }
    }

    private void handleSetPosition(SetPosition message) {
        if (message.animated && !controlCons.running) {
            engine.setPositionAnimated(message.position);
        } else {
            if (controlCons.running == false && engine.isRunning()) {
                // if setting position while animation is still running, it needs to be stopped first
                engine.setPlayConstraints(controlCons, message.position);
            } else {
                engine.setPosition(message.position);
            }
        }
    }

    private void handleSetSpeed(SetSpeed message) {
        controlCons.setSpeed(message.newSpeed);
        engine.setPlayConstraints(controlCons, null);
    }

    private void handleStatusRequest(StatusRequest message) {
        Long position = null;
        if (message.position != null) {
            position = message.position._;
        }
        if (message.positionMin != null) {
            controlCons.setTimerMin(message.positionMin._);
        }
        if (message.positionMax != null) {
            controlCons.setTimerMax(message.positionMax._);
        }
        if (message.speed != null) {
            controlCons.setSpeed(message.speed._);
        }
        if (message.mute != null) {
            controlCons.mute = message.mute._;
        }
        engine.setPlayConstraints(controlCons, position);
        if (message.responseTo != null) {
            position = engine.getPosition();
            message.responseTo.send(new StatusResponse(position, controlCons.timerMin, controlCons.timerMax,
                    controlCons.playerSpeed));
        }
    }

    private void setState(State state) {
        if (this.state != state) {
            this.state = state;
            sendUpdate(new StateUpdate(state));
        }
    }

    @Override
    protected void idle() {
        super.idle();

        // simpler first running version: no synchronization, just 5 frames ahead (the first 5 on setting position)
        // prefetching TODO (+ initial prefetching on position request & initialization, -> delayed start after the minimum has arrived, sent to sound and sound responded it started)

        engine.updateFrameInScreenIfTimeHasCome();
    }

    /**
     * The engine knows exactly what's happening to the screen. For animations this can be independent of the player
     * control. This class does everything related to shipping around frames.
     */
    private class Engine {
        private Comparator<CachedFrame> cachedFrameComparator = new Comparator<CachedFrame>() {
            @Override
            public int compare(CachedFrame o1, CachedFrame o2) {
                if (timer.speed > 0.0) {
                    return o1.seqNum < o2.seqNum ? -1 : 1;
                } else {
                    return o2.seqNum < o1.seqNum ? -1 : 1;
                }
            }
        };
        private PriorityQueue<CachedFrame> queuedFrames = new PriorityQueue<CachedFrame>(MAX_EXPECTED_PREFETCH_SIZE,
                cachedFrameComparator);
        private Timer timer = new Timer();
        private long nextSeqNumExpected = -1;
        private long startingTimerPos = 0;
        private Long anotherPosSetDuringPreparing = null;

        private PlayConstraints engineCons = new PlayConstraints();

        private State state = State.STOPPED;

        public void start() {
            // TODO maybe has to wait until frames are available (cachedFrames non empty)
            if (!timer.isRunning()) {
                setState(State.PREPARING);
                startingTimerPos = timer.getPosition();
                long currentSeqNum = calculateSeqNum(startingTimerPos);
                // if at end: start playing from beginning
                if (timer.isPlayingForward() && currentSeqNum >= controlCons.endFrameSeqNum
                        || !timer.isPlayingForward() && currentSeqNum <= controlCons.startFrameSeqNum) {
                    long resetPosition = timer.isPlayingForward() ? controlCons.timerMin : controlCons.timerMax;
                    timer.setPosition(resetPosition);
                    startingTimerPos = resetPosition;
                    currentSeqNum = calculateSeqNum(resetPosition);
                }
                timer.setSpeed(engineCons.playerSpeed);
                startFetching(currentSeqNum);
                timer.start();
                // timer will be repositioned in handleAudioSyncEvent
            }
        }

        public void stop() {
            if (timer.isRunning()) {
                setState(State.STOPPED);
                timer.stop();
                resetFetching();
            }
        }

        private void restart(Long newPosition) {
            resetFetching();

            long position;
            if (newPosition != null)
                position = newPosition;
            else
                position = timer.getPosition();
            startingTimerPos = position;
            long positionSeqNum = calculateSeqNum(position);
            startFetching(positionSeqNum);
        }

        public void synchronize() {
            setState(State.PLAYING);
            if (anotherPosSetDuringPreparing != null) {
                // handle SetPosition that was queued, instead of syncing (will change anyway)
                long newPosition = anotherPosSetDuringPreparing;
                anotherPosSetDuringPreparing = null;
                setPosition(newPosition);
            } else {
                // sync video to sound
                timer.setPosition(startingTimerPos + SYNC_OFFSET);
            }
        }

        private long getPosition() {
            return timer.getPosition();
        }

        public void setPosition(long newPosition) {
            startingTimerPos = newPosition;
            long positionSeqNum = calculateSeqNum(startingTimerPos);
            //System.out.println("positionSeqNum: " + positionSeqNum + " derived from " + message.position);

            if (timer.isRunning()) {
                if (state == State.PREPARING) {
                    // if already in PREPARING: remember for later to avoid running out of buffers (due to overload)
                    anotherPosSetDuringPreparing = newPosition;
                } else {
                    setState(State.PREPARING);
                    // first flush everything
                    resetFetching();

                    // send fetching requests
                    startFetching(positionSeqNum);
                }
            } else {
                sendFetchRequest(positionSeqNum, true);
            }

            // move timer
            timer.setPosition(startingTimerPos);
            sendUpdate(new PositionUpdate(startingTimerPos, controlCons.timerMin, controlCons.timerMax));
        }

        private void sendFetchRequest(long seqNum, boolean onlyIfFreeResources) {
            frameCache.send(new FrameRequest(seqNum, USAGE_COUNT, onlyIfFreeResources, Controller.this));
        }

        public void setPositionAnimated(long newPosition) {
            long position = getPosition();
            double speed = (position < newPosition) ? ANIMATION_SPEED : -ANIMATION_SPEED;
            PlayConstraints cons = new PlayConstraints();
            cons.running = true;
            cons.looping = false;
            cons.mute = true;
            cons.setSpeed(speed);
            if (position < newPosition) {
                cons.setTimerMin(null);
                cons.setTimerMax(newPosition);
            } else {
                cons.setTimerMin(newPosition);
                cons.setTimerMax(null);
            }
            cons.positionUpdates = false;
            setPlayConstraints(cons, null);
            // sending update here, instead of normal updates
            sendUpdate(new PositionUpdate(newPosition, controlCons.timerMin, controlCons.timerMax));
        }

        private void setState(State state) {
            if (this.state != state) {
                this.state = state;
            }
        }

        public boolean isRunning() {
            return timer.isRunning();
        }

        public void setPlayConstraints(PlayConstraints cons, Long position) {
            // TODO decide if restart, etc. is necessary and apply constraints
            boolean runningStateChanged = cons.running != timer.isRunning();
            boolean restartNeeded = cons.running && position != null;
            // a range update is not really needed here, because the update is only for controllers, so it has to be sent on a higher level
            boolean rangeUpdateNeeded = false;
            boolean positionUpdateNeeded = position != null;

            // apply all attributes (and decide if restart is needed)
            if (timer.getSpeed() != cons.playerSpeed) {
                timer.setSpeed(cons.playerSpeed);
                restartNeeded = restartNeeded || cons.running;
            }
            if (engineCons.timerMin != cons.timerMin) {
                rangeUpdateNeeded = true;
                restartNeeded = restartNeeded || cons.running;
            }
            if (engineCons.timerMax != cons.timerMax) {
                rangeUpdateNeeded = true;
                restartNeeded = restartNeeded || cons.running;
            }
            if (engineCons.mute != cons.mute) {
                restartNeeded = restartNeeded || cons.running;
            }

            System.out.println("DEBUG: runningStateChanged = " + runningStateChanged + "; restartNeeded = "
                    + restartNeeded + "; already running = " + timer.isRunning());

            engineCons.copyFrom(cons);

            if (runningStateChanged) {
                if (cons.running) {
                    if (position != null) {
                        timer.setPosition(position);
                    }
                    start();
                } else {
                    stop();
                    if (position != null) {
                        setPosition(position);
                    }
                }
            } else if (restartNeeded) {
                restart(position);
            } else {
                if (position != null) {
                    setPosition(position);
                }
            }
        }

        private void handleCachedFrame(CachedFrame frame) {
            System.out.println("TRACE: " + frame.seqNum + " received in controller");
            if (state == State.PREPARING || state == State.PLAYING) {
                if (timer.speed > 0.0
                        && (frame.seqNum < nextSeqNumExpected || frame.seqNum > nextSeqNumExpected
                                + engineCons.prefetchSize)
                        || timer.speed < 0.0
                        && (frame.seqNum > nextSeqNumExpected || frame.seqNum < nextSeqNumExpected
                                - engineCons.prefetchSize)) {
                    System.out.println("DEBUG: dropping frame " + frame.seqNum + " because expected "
                            + nextSeqNumExpected);
                    // silently drop (was too late, no longer needed)
                    recycle(frame, USAGE_COUNT);
                    return;
                }
                if (frame.seqNum == nextSeqNumExpected) {
                    long newSeqNum = nextSeqNumExpected;
                    // find next seq num that hasn't been received yet
                    while (true) {
                        newSeqNum = newSeqNum + timer.getSpeedDirection();
                        boolean alreadyReceived = false;
                        for (CachedFrame f : queuedFrames) {
                            if (f.seqNum == newSeqNum) {
                                alreadyReceived = true;
                                break;
                            }
                        }
                        if (!alreadyReceived)
                            break;
                    }
                    nextSeqNumExpected = newSeqNum;
                }
                System.out.println("TRACE: " + frame.seqNum + ": added to queued frames");
                queuedFrames.add(frame);
                if (!engineCons.mute) {
                    audioRenderer.send(frame);
                } else {
                    frame.recycle();
                }
            } else { // result of moving position in stopped state -> immediately display
                // already recycle once because they're not sent to audio renderer
                frame.recycle();
                videoRenderer.send(frame);
            }
        }

        private void startFetching(long positionSeqNum) {
            prefetch(positionSeqNum);
            if (!engineCons.mute) {
                audioRenderer.send(new SetSpeed(engineCons.playerSpeed));
                audioRenderer.send(new ControlCommand(Command.START));
            }
        }

        public void prefetch(long positionSeqNum) {
            System.out.println("DEBUG: " + positionSeqNum + ": prefetching");
            nextSeqNumExpected = positionSeqNum;
            long speedDirection = timer.getSpeedDirection();
            int maxN;
            if (speedDirection > 0 && positionSeqNum + engineCons.prefetchSize > engineCons.endFrameSeqNum)
                maxN = (int) (engineCons.endFrameSeqNum - positionSeqNum + 1);
            else if (speedDirection < 0 && positionSeqNum - engineCons.prefetchSize < engineCons.startFrameSeqNum)
                maxN = (int) (positionSeqNum - engineCons.startFrameSeqNum + 1);
            else
                maxN = engineCons.prefetchSize;
            for (int i = 0; i < maxN; i++) {
                sendFetchRequest(positionSeqNum + i * speedDirection, false);
            }
        }

        public void reset() {
            nextSeqNumExpected = -1;
            clearQueuedFrames();

        }

        public void resetFetching() {
            if (!engineCons.mute) {
                audioRenderer.send(new ControlCommand(Command.STOP));
                audioRenderer.send(new ControlCommand(Command.FLUSH));
            }
            clearQueuedFrames();
        }

        public void clearQueuedFrames() {
            for (CachedFrame cf : queuedFrames) {
                cf.recycle();
            }
            queuedFrames.clear();
        }

        private void recycle(CachedFrame cf, int times) {
            for (int i = 0; i < times; i++) {
                cf.recycle();
            }
        }

        public void updateFrameInScreenIfTimeHasCome() {

            // timing rendering of video frames
            if (timer.isRunning()) {
                // paint current frame if it changed
                long currentTime = timer.getPosition();
                long currentSeqNum = calculateSeqNum(currentTime);
                if (queuedFrames.isEmpty()) {
                    if (timer.isPlayingForward() && currentSeqNum >= engineCons.endFrameSeqNum
                            || !timer.isPlayingForward() && currentSeqNum <= engineCons.startFrameSeqNum) { //
                        if (engineCons.looping) {
                            long resetPosition = timer.isPlayingForward() ? engineCons.timerMin : engineCons.timerMax;
                            //handleSetPosition(new SetPosition(resetPosition));
                            restart(resetPosition);
                        } else {
                            //handleControlCommand(new ControlCommand(Command.STOP));
                            engineCons.running = false;
                            stop();
                            // also stopping outer control state
                            controlCons.running = false;
                            Controller.this.setState(State.STOPPED);
                        }
                    }
                } else {
                    long frameSeqNum = queuedFrames.peek().seqNum;
                    //System.out.println("TRACE: currentSeqNum = " + currentSeqNum + " (based on current time = "
                    //        + currentTime + "); queuedFrames.peek().seqNum = " + frameSeqNum);
                    if (timer.isFirstBeforeSecondOrEqual(frameSeqNum, currentSeqNum)) { // <= 
                        CachedFrame frame = queuedFrames.poll();
                        // if speed 2 or above, only display every second frame, 4 or above only display every 4th
                        if (timer.getSpeed() < 2.0 ||
                                timer.getSpeed() < 3.0 && frame.seqNum % 2 == 0 ||
                                timer.getSpeed() < 4.0 && frame.seqNum % 3 == 0 ||
                                frame.seqNum % 4 == 0) {
                            videoRenderer.send(frame);
                        } else {
                            frame.recycle();
                        }
                        long prefetchSeqNum = frameSeqNum + engineCons.prefetchSize * timer.getSpeedDirection();
                        if (prefetchSeqNum >= engineCons.startFrameSeqNum
                                && prefetchSeqNum <= engineCons.endFrameSeqNum) {
                            sendFetchRequest(prefetchSeqNum, false);
                            System.out.println("TRACE: " + prefetchSeqNum + ": sent fetch request");
                        }
                        // event must be sent with min and max of control (user is not aware of inner timerMin and timerMax)
                        if (engineCons.positionUpdates) {
                            sendUpdate(new PositionUpdate(currentTime, controlCons.timerMin, controlCons.timerMax));
                        }
                    }
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

        public boolean isPlayingForward() {
            return speed > 0.0;
        }

        public boolean isFirstBeforeSecond(long a, long b) {
            if (speed > 0.0) {
                return a < b;
            } else {
                return a > b;
            }
        }

        public boolean isFirstBeforeSecondOrEqual(long a, long b) {
            if (speed > 0.0) {
                return a <= b;
            } else {
                return a >= b;
            }
        }

        public long getSpeedDirection() {
            return (long) Math.signum(speed);
        }
    }

    /**
     * @return the videoRenderer
     */
    public VideoRenderer getVideoRenderer() {
        return videoRenderer;
    }
}
