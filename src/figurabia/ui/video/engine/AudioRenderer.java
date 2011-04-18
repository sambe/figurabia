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
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.messages.CachedFrame;
import figurabia.ui.video.engine.messages.ControlCommand;
import figurabia.ui.video.engine.messages.RecyclingBag;

public class AudioRenderer extends Actor {

    private final SourceDataLine line;
    private final Queue<CachedFrame> frameQueue = new LinkedList<CachedFrame>();
    private int bufferPos = 0;

    private final Actor recycler;

    public AudioRenderer(Actor errorHandler, AudioFormat audioFormat, Actor recycler)
            throws LineUnavailableException {
        super(errorHandler);
        this.recycler = recycler;

        // create sound channel
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
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
                    recycler.send(new RecyclingBag(cf));
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
            System.out.println("written " + bytesWritten + " bytes to audio line");

            bufferPos += bytesWritten;
            if (bufferPos == audioBuffer.getLength() + audioBuffer.getOffset()) {
                System.err.println("DEBUG: Finished playing audio for frame "
                        + frameQueue.peek().seqNum);
                recycler.send(new RecyclingBag(frameQueue.poll()));
                bufferPos = 0;
            }
        }
    }

    private void handleMediaFrame(CachedFrame frame) {
        frameQueue.add(frame);
    }

    @Override
    protected void destruct() {
        // close sound channel
        line.close();
    }
}
