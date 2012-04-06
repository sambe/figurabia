/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 06.04.2012
 */
package figurabia.ui.video.engine.audio;

import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineEvent.Type;

public class JavaSoundAudioProvider implements AudioProvider {

    private AudioFormat audioFormat;
    private SourceDataLine line;

    private List<AudioProvider.AudioStateListener> audioStateListeners = new ArrayList<AudioStateListener>();
    private boolean open = false;

    public JavaSoundAudioProvider() {

    }

    @Override
    public void open(AudioFormat audioFormat) {
        // create sound channel
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
        line.addLineListener(new LineListener() {
            @Override
            public void update(LineEvent event) {
                if (event.getType().equals(Type.START)) {
                    System.err.println("Received LineEvent: START");
                    notifyAudioStateListeners(AudioState.PLAYING);
                } else if (event.getType().equals(Type.STOP)) {
                    System.err.println("Received LineEvent: STOP");
                    notifyAudioStateListeners(AudioState.STOPPED);
                } else {
                    System.err.println("Received LineEvent: " + event.getType());
                }
            }
        });

        // TODO do something with these controls
        FloatControl mixerSourceLineGainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        BooleanControl mixerSourceLineMuteControl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
        FloatControl mixerSourceLinePanControl = (FloatControl) line.getControl(FloatControl.Type.PAN);
        FloatControl mixerSourceLineSampleRateControl = (FloatControl) line.getControl(FloatControl.Type.SAMPLE_RATE);

        open = true;
    }

    @Override
    public void close() {
        // close sound channel
        if (line.isOpen()) {
            line.close();
        }
        open = false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void flush() {
        line.flush();
    }

    @Override
    public int getWritableBytes() {
        return line.available();
    }

    @Override
    public boolean isPlaying() {
        return line.isRunning();
    }

    @Override
    public void pause() {
        line.stop();
    }

    @Override
    public void start() {
        line.start();
    }

    @Override
    public void stop() {
        line.stop();
    }

    @Override
    public int write(byte[] data, int offset, int length) {
        return line.write(data, offset, length);
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
