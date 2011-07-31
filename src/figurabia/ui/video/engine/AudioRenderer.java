/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 26.12.2010
 */
package figurabia.ui.video.engine;

import java.util.LinkedList;
import java.util.Queue;

import javax.media.Buffer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineEvent.Type;

import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.messages.AudioSyncEvent;
import figurabia.ui.video.engine.messages.CachedFrame;
import figurabia.ui.video.engine.messages.ControlCommand;

public class AudioRenderer extends Actor {

    private AudioFormat audioFormat;
    private SourceDataLine line;
    private final Queue<CachedFrame> frameQueue = new LinkedList<CachedFrame>();
    private int bufferPos = 0;

    private final Actor syncTarget;

    public AudioRenderer(Actor errorHandler, AudioFormat audioFormat, Actor syncTarget) {
        super(errorHandler);
        this.audioFormat = audioFormat;
        this.syncTarget = syncTarget;
    }

    @Override
    protected void init() throws Exception {
        // create sound channel
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);

        // add listener for synchronization events
        if (syncTarget != null) {
            line.addLineListener(new LineListener() {
                @Override
                public void update(LineEvent event) {
                    if (event.getType().equals(Type.START)) {
                        System.err.println("Received LineEvent: START");
                        syncTarget.send(new AudioSyncEvent(AudioSyncEvent.Type.START));
                    } else if (event.getType().equals(Type.STOP)) {
                        System.err.println("Received LineEvent: STOP");
                        syncTarget.send(new AudioSyncEvent(AudioSyncEvent.Type.STOP));
                    } else {
                        System.err.println("Received LineEvent: " + event.getType());
                    }
                }
            });
        }

        // TODO do something with these controls
        FloatControl mixerSourceLineGainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        BooleanControl mixerSourceLineMuteControl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
        FloatControl mixerSourceLinePanControl = (FloatControl) line.getControl(FloatControl.Type.PAN);
        FloatControl mixerSourceLineSampleRateControl = (FloatControl) line.getControl(FloatControl.Type.SAMPLE_RATE);
    }

    @Override
    protected void act(Object message) {
        if (message instanceof CachedFrame) {
            //System.out.println("AudioRenderer: receiving media frame");
            handleMediaFrame((CachedFrame) message);
        } else if (message instanceof ControlCommand) {
            ControlCommand c = (ControlCommand) message;
            System.out.println("AudioRenderer: receiving ControlCommand: " + c.command);
            switch (c.command) {
            case START:
                System.err.println("AudioRenderer: starting...");
                line.start();
                break;
            case STOP:
                line.stop();
                break;
            case FLUSH:
                // recycle all frames in the queue
                for (CachedFrame cf : frameQueue) {
                    cf.recycle();
                }
                frameQueue.clear();
                line.flush();
                break;
            case CLOSE:
                // stops the actor (which will close the line)
                stop();
                break;
            }
        } else {
            throw new IllegalArgumentException("unknown type of message: " + message.getClass().getName());
        }
    }

    @Override
    protected void idle() {
        //System.out.println("AudioRenderer: started writing to buffer");
        fillAudioBuffer();
        //System.out.println("AudioRenderer: finished writing to buffer");

        super.idle();
    }

    private void fillAudioBuffer() {
        int available;
        if (frameQueue.peek() != null && (available = line.available()) != 0) {
            Buffer audioBuffer = frameQueue.peek().frame.audio.getBuffer();
            int offset = audioBuffer.getOffset() + bufferPos;
            int length = audioBuffer.getLength() - bufferPos;
            if (length > available) {
                length = available;
            }
            //System.out.println("writing: length = " + length + "; bufferPos = " + bufferPos + "; offset = " + offset
            //        + "; buffer.length = " + audioBuffer.getLength() + "; buffer.offset = " + audioBuffer.getOffset());
            //System.out.println("AudioRenderer: before writing to line: available = " + available + "; length = "
            //        + length);
            int bytesWritten = line.write((byte[]) audioBuffer.getData(), offset, length);
            //System.out.println("AudioRenderer: after writing to line: bytesWritten: " + bytesWritten);
            //System.out.println("written " + bytesWritten + " bytes to audio line");

            bufferPos += bytesWritten;
            if (bufferPos == audioBuffer.getLength() + audioBuffer.getOffset()) {
                //System.err.println("DEBUG: Finished playing audio for frame "
                //        + frameQueue.peek().seqNum);
                frameQueue.poll().recycle();
                bufferPos = 0;
            }
        }
    }

    private void handleMediaFrame(CachedFrame frame) {
        if (!frame.frame.isEndOfMedia()) {
            frameQueue.add(frame);
        } else {
            frame.recycle();
        }
    }

    @Override
    protected void destruct() {
        // close sound channel
        line.close();
    }
}
