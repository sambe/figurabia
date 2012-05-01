/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 06.04.2012
 */
package figurabia.ui.video.engine.audio;

import javax.sound.sampled.AudioFormat;

/**
 * Defines the expectations towards an audio sub system
 * 
 * @author Samuel Berner
 */
public interface AudioProvider {

    /**
     * Opens the audio line and prepares everything for playing audio in the given format.
     * 
     * @param audioFormat the format of the audio that will be played
     */
    void open(AudioFormat audioFormat);

    /**
     * @return whether the audio provider was opened, but not yet closed.
     */
    boolean isOpen();

    /**
     * Closes the audio line.
     */
    void close();

    /**
     * Returns how many bytes can still be written to the line (until buffers are full). If the returned value is > 0,
     * it can actually be less than the total space left. When writing data, just repeatedly call this method after each
     * write until it returns 0.
     * 
     * @return the maximum number of bytes that can be written at once (in the current state)
     */
    int getWritableBytes();

    /**
     * Writes audio data to the line.
     * 
     * @param data the audio data
     * @param offset the offset in the audio data
     * @param length the length of the audio data to write
     * @return
     */
    int write(byte[] data, int offset, int length);

    /**
     * Starts playback on the line. Only call once buffer has been filled.
     */
    void start();

    /**
     * Stops playback.
     */
    void stop();

    /**
     * Pauses playback.
     */
    void pause();

    /**
     * Clear all buffers.
     */
    void flush();

    boolean isPlaying();

    public enum AudioState {
        STOPPED, PLAYING
    }

    public interface AudioStateListener {
        void updateState(AudioState audioState);
    }

    public void addAudioStateListener(AudioStateListener l);

    public void removeAudioStateListener(AudioStateListener l);
}
