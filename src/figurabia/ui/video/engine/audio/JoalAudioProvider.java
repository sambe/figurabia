/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 25.03.2012
 */
package figurabia.ui.video.engine.audio;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.AudioFormat;

import com.jogamp.openal.AL;
import com.jogamp.openal.ALConstants;
import com.jogamp.openal.ALException;
import com.jogamp.openal.ALFactory;
import com.jogamp.openal.util.ALut;

/**
 * This class is supposed to contain all JOAL specific code to provide a clear and simple interface for the
 * AudioRenderer actor.
 * 
 * @author Samuel Berner
 */
public class JoalAudioProvider implements AudioProvider {

    static AL al = null;

    static {
        // Initialize OpenAL and clear the error bit.
        try {
            ALut.alutInit();
            al = ALFactory.getAL();
            al.alGetError();
        } catch (ALException e) {
            System.err.println("ERROR: Error initializing OpenAL");
            e.printStackTrace();
        }
    }

    private static boolean debug = true;

    private static void debugMsg(String str) {
        if (debug)
            System.err.println(str);
    }

    // The size of a chunk from the stream that we want to read for each update.
    private static int MAX_BUFFER_SIZE = 4096 * 16;

    // The number of buffers used in the audio pipeline
    private static int NUM_BUFFERS = 12;

    private static int MIN_BUFFERS_PREFETCHING = 3;

    // Buffers hold sound data. There are two of them by default (front/back)
    private int[] buffers = new int[NUM_BUFFERS];
    private boolean[] bufferIsQueued = new boolean[NUM_BUFFERS];

    // Sources are points emitting sound.
    private int[] source = new int[1];

    private int format; // OpenAL data format
    private int rate; // sample rate

    // Position, Velocity, Direction of the source sound.
    private float[] sourcePos = { 0.0f, 0.0f, 0.0f };
    private float[] sourceVel = { 0.0f, 0.0f, 0.0f };
    private float[] sourceDir = { 0.0f, 0.0f, 0.0f };

    private int bufferQueuedNext = 0;
    private int bufferUnqueuedNext = -1;

    private List<AudioProvider.AudioStateListener> audioStateListeners = new ArrayList<AudioStateListener>();

    private boolean open = false;
    private boolean clientPlaying = false;
    private boolean actualPlaying = false;
    private int prefetched = 0;
    private boolean syncEvent = false;

    /**
     * Initialize OpenAL based on the given audio format
     */
    public void open(AudioFormat audioFormat) {

        int numChannels = audioFormat.getChannels();
        int numBytesPerSample = audioFormat.getSampleSizeInBits() / 8;

        if (numBytesPerSample == 1) {
            if (numChannels == 1)
                format = AL.AL_FORMAT_MONO8;
            else
                format = AL.AL_FORMAT_STEREO8;
        } else {
            if (numChannels == 1)
                format = AL.AL_FORMAT_MONO16;
            else
                format = AL.AL_FORMAT_STEREO16;
        }

        rate = (int) audioFormat.getSampleRate();

        System.err.println("DEBUG: #Buffers: " + NUM_BUFFERS);
        System.err.println("DEBUG: Format: 0x" + Integer.toString(format, 16));

        al.alGenBuffers(NUM_BUFFERS, buffers, 0);
        check();
        al.alGenSources(1, source, 0);
        check();

        al.alSourcefv(source[0], AL.AL_POSITION, sourcePos, 0);
        al.alSourcefv(source[0], AL.AL_VELOCITY, sourceVel, 0);
        al.alSourcefv(source[0], AL.AL_DIRECTION, sourceDir, 0);

        al.alSourcef(source[0], AL.AL_ROLLOFF_FACTOR, 0.0f);
        al.alSourcei(source[0], AL.AL_SOURCE_RELATIVE, AL.AL_TRUE);

        open = true;
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * OpenAL cleanup
     */
    public void close() {
        al.alSourceStop(source[0]);
        flush();

        al.alDeleteBuffers(NUM_BUFFERS, buffers, 0);
        check();

        al.alDeleteSources(1, source, 0);
        check();
        open = false;
    }

    /**
     * Check if the source is playing
     */
    public boolean isPlaying() {
        int[] state = new int[1];
        al.alGetSourcei(source[0], AL.AL_SOURCE_STATE, state, 0);
        return (state[0] == AL.AL_PLAYING);
    }

    /**
     * Empties the queue
     */
    public void flush() {
        int[] queued = new int[1];

        al.alGetSourcei(source[0], AL.AL_BUFFERS_QUEUED, queued, 0);
        debugMsg("Unqueing all queued buffers (" + queued[0] + ")");

        while (queued[0] > 0) {
            int[] buffer = new int[1];

            al.alSourceUnqueueBuffers(source[0], 1, buffer, 0);
            check();
            bufferIsQueued[indexOf(buffers, buffer[0])] = false;

            queued[0]--;
        }
        bufferQueuedNext = 0;
        bufferUnqueuedNext = -1;
        if (!isAll(bufferIsQueued, false))
            throw new IllegalStateException("Flushed, but not all buffers were unqueued");
    }

    /**
     * Check for OpenAL errors...
     */
    private void check() {
        int error = al.alGetError();
        if (error != AL.AL_NO_ERROR) {
            String errorName = generateErrorName(error);
            throw new ALException("OpenAL error raised... " + errorName);
        }
    }

    private String generateErrorName(int error) {
        String errorName = "(code " + Integer.toHexString(error) + ")";
        for (Field f : ALConstants.class.getDeclaredFields()) {
            if (f.getType() != int.class)
                continue;
            try {
                int value = (Integer) f.get(null);
                if (value == error) {
                    errorName = f.getName();
                    break;
                }
            } catch (IllegalArgumentException e) {
                // don't care
            } catch (IllegalAccessException e) {
                // don't care
            } catch (NullPointerException e) {
                // don't care
                System.err.println("Unnecessary null pointer exception, because field " + f.getName()
                        + " is not static");
            }
        }
        return errorName;
    }

    public void start() {
        if (!clientPlaying) {
            clientPlaying = true;
            syncEvent = true;
            // only start immediately if there are some buffers queued already
            if (bufferUnqueuedNext != -1) {
                start_();
            } else {
                prefetched = 0;
            }
        }
    }

    private void start_() {
        al.alSourcePlay(source[0]);
        if (al.alGetError() == AL.AL_NO_ERROR) {
            debugMsg("Started playback");
            if (syncEvent) {
                syncEvent = false;
                notifyAudioStateListeners(AudioState.PLAYING);
            }
        }
        actualPlaying = true;
    }

    public void stop() {
        if (clientPlaying) {
            clientPlaying = false;
            if (actualPlaying) {
                al.alSourceStop(source[0]);
                if (al.alGetError() == AL.AL_NO_ERROR) {
                    notifyAudioStateListeners(AudioState.STOPPED);
                }
                actualPlaying = false;
            }
        }
    }

    public void pause() {
        al.alSourcePause(source[0]);
        if (al.alGetError() == AL.AL_NO_ERROR) {
            notifyAudioStateListeners(AudioState.STOPPED);
        }
    }

    private int indexOf(int[] array, int num) {
        for (int i = 0; i < array.length; i++) {
            if (num == array[i])
                return i;
        }
        return -1;
    }

    private boolean isAll(boolean[] array, boolean value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] != value)
                return false;
        }
        return true;
    }

    private void reclaimBufferSpace() {
        // if no buffers are queued, none can be unqueued anyway (avoids an IndexOutOfBoundsException (occurs as a follow up error))
        if (bufferUnqueuedNext == -1)
            return;

        // check if buffers can be unqueued
        int[] processed = new int[1];
        al.alGetSourcei(source[0], AL.AL_BUFFERS_PROCESSED, processed, 0);

        if (processed[0] > 0) {
            while (processed[0] > 0) {
                int[] buffer = new int[1];
                al.alSourceUnqueueBuffers(source[0], 1, buffer, 0);
                check();
                int bufferIndex = indexOf(buffers, buffer[0]);
                if (bufferIndex == -1)
                    throw new IndexOutOfBoundsException("searched for " + buffer[0] + " in buffers "
                            + Arrays.toString(buffers));
                bufferIsQueued[bufferIndex] = false;
                if (buffer[0] != buffers[bufferUnqueuedNext])
                    throw new IllegalStateException("Unexpected buffer unqueued: expected: " + bufferUnqueuedNext
                            + "; actual: " + buffer[0]);
                debugMsg("Unqueued buffer " + bufferUnqueuedNext);
                bufferUnqueuedNext = (bufferUnqueuedNext + 1) % NUM_BUFFERS;
                processed[0]--;
            }
            // To avoid a bad state, we have to unset bufferUnqueuedNext in case all buffers have been unqueued.
            if (bufferUnqueuedNext == bufferQueuedNext) {
                bufferUnqueuedNext = -1;
                if (!isAll(bufferIsQueued, false))
                    throw new IllegalStateException("Flushed, but not all buffers were unqueued");
                actualPlaying = false;
                prefetched = 0;
                debugMsg("Actual playback has stopped (all buffers processed): reset to restart");
            }
        }
    }

    public int getWritableBytes() {
        reclaimBufferSpace();
        return getWritableBytes_();
    }

    private int getWritableBytes_() {
        // do not start filling a buffer that has not yet been unqueued
        if (bufferQueuedNext == bufferUnqueuedNext)
            return 0;
        return MAX_BUFFER_SIZE;
    }

    public int write(byte[] data, int offset, int length) {
        int bytesToWrite = Math.min(getWritableBytes(), length);

        // write
        if (bytesToWrite > 0) {
            if (bufferIsQueued[bufferQueuedNext])
                throw new IllegalStateException("already queued: " + bufferQueuedNext);
            ByteBuffer dataBuffer = ByteBuffer.wrap(data, offset, bytesToWrite);
            al.alBufferData(buffers[bufferQueuedNext], format, dataBuffer, bytesToWrite, rate);
            check();
            debugMsg("Wrote " + bytesToWrite + " bytes to buffer " + bufferQueuedNext);

            // to play buffer must be queued
            al.alSourceQueueBuffers(source[0], 1, buffers, bufferQueuedNext);
            check();
            bufferIsQueued[bufferQueuedNext] = true;
            debugMsg("Queued buffer " + bufferQueuedNext);
            if (bufferUnqueuedNext == -1)
                bufferUnqueuedNext = bufferQueuedNext;
            bufferQueuedNext = (bufferQueuedNext + 1) % NUM_BUFFERS;
        }
        // start playback if it is supposed to be started
        if (clientPlaying && !actualPlaying) {
            if (prefetched >= MIN_BUFFERS_PREFETCHING)
                start_();
            else
                prefetched++;
        }

        return bytesToWrite;
    }

    @Override
    public void addAudioStateListener(AudioStateListener l) {
        audioStateListeners.add(l);
    }

    @Override
    public void removeAudioStateListener(AudioStateListener l) {
        audioStateListeners.remove(l);
    }

    protected void notifyAudioStateListeners(AudioState audioState) {
        for (AudioStateListener l : audioStateListeners) {
            try {
                l.updateState(audioState);
            } catch (RuntimeException e) {
                System.err.println("ERROR: Exception thrown in audio state listener of JoalAudioProvider:");
                e.printStackTrace();
            }
        }
    }
}
