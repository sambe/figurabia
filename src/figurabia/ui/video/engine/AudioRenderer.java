/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 26.12.2010
 */
package figurabia.ui.video.engine;

import java.util.LinkedList;
import java.util.Queue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.LineEvent.Type;

import figurabia.ui.video.access.AudioBuffer;
import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.messages.AudioSyncEvent;
import figurabia.ui.video.engine.messages.CachedFrame;
import figurabia.ui.video.engine.messages.ControlCommand;
import figurabia.ui.video.engine.messages.SetSpeed;

public class AudioRenderer extends Actor {

    private static final double MIN_SPEED = 0.25;

    private AudioFormat audioFormat;
    private SourceDataLine line;
    private final Queue<CachedFrame> frameQueue = new LinkedList<CachedFrame>();
    private int bufferPos = 0;
    private int bytesToWrite = -1;
    private double speed = 1.0;
    private byte[] speedCorrectedBuffer = null;
    private long speedCorrectedBufferSeqNum = -1;

    private final Actor syncTarget;

    public AudioRenderer(Actor errorHandler, AudioFormat audioFormat, Actor syncTarget) {
        super(errorHandler, 50000000);
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
            handleControlCommand((ControlCommand) message);
        } else if (message instanceof SetSpeed) {
            handleSetSpeed((SetSpeed) message);
        } else {
            throw new IllegalArgumentException("unknown type of message: " + message.getClass().getName());
        }
    }

    private void handleControlCommand(ControlCommand c) {
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
    }

    @Override
    protected void idle() {
        //System.out.println("AudioRenderer: started writing to buffer");
        fillAudioBuffer();
        //System.out.println("AudioRenderer: finished writing to buffer");

        super.idle();
    }

    private void checkBufferSize(byte[] frameBuffer, double absSpeed) {
        if (speedCorrectedBuffer == null || speedCorrectedBuffer.length < frameBuffer.length / absSpeed) {
            // calculating with the min possible speed, or lower if actual is lower
            double minSpeed = Math.min(absSpeed, MIN_SPEED);
            int length = (int) Math.ceil(frameBuffer.length / minSpeed);
            speedCorrectedBuffer = new byte[length];
        }

    }

    private int copyToSpeedScaledBuffer(byte[] idata, byte[] odata, long seqNum) {
        //byte[] idata = (byte[]) inputBuffer.array();
        //byte[] odata = (byte[]) outputBuffer.array();

        boolean forward = speed > 0.0;
        double absSpeed = Math.abs(speed);

        // determine length
        int frameBytes = audioFormat.getFrameSize();
        int inputFrames = idata.length / frameBytes;
        long start = (long) Math.floor(seqNum * inputFrames / absSpeed);
        long end = (long) Math.floor((seqNum + 1) * inputFrames / absSpeed);
        int outputFrames = (int) (end - start);
        int outputBytes = outputFrames * frameBytes;

        // scale
        //outputBuffer.position(0);
        //outputBuffer.limit(outputBytes);
        for (int i = 0; i < outputFrames; i++) {
            // currently only picking nearest, could be linear or polynomial interpolation
            int nearestInputFrame = i * inputFrames / outputFrames;
            int ibase = nearestInputFrame * frameBytes;
            int obase = forward ? i * frameBytes : (outputFrames - 1 - i) * frameBytes;
            for (int j = 0; j < frameBytes; j++) {
                odata[obase + j] = idata[ibase + j];
                //outputBuffer.put(obase + j, inputBuffer.get(ibase + j));
            }
        }
        return outputBytes;
    }

    private void fillAudioBuffer() {
        // TODO if speed != 1.0, we need to copy the data in a different fashion (approximation -> later offer different modes of approximation (e.g. nearest, average, polynomial)
        int available;
        while (frameQueue.peek() != null && (available = line.available()) != 0) {
            if (!line.isOpen()) {
                throw new IllegalStateException("Line was not started, but is already delivered with audio data.");
            }
            AudioBuffer audio = frameQueue.peek().frame.audio;
            byte[] audioBuffer = audio.getAudioData();

            // if speed != 1.0 replace with speed corrected buffer
            checkBufferSize(audioBuffer, Math.abs(speed));
            if (speed == 1.0) {
                bytesToWrite = audioBuffer.length;
            } else {
                long seqNum = frameQueue.peek().seqNum;
                if (seqNum != speedCorrectedBufferSeqNum) {
                    bytesToWrite = copyToSpeedScaledBuffer(audioBuffer, speedCorrectedBuffer, seqNum);
                    speedCorrectedBufferSeqNum = seqNum;
                }
                audioBuffer = speedCorrectedBuffer;
            }

            // select part of buffer to write to line out
            int offset = bufferPos;
            int length = bytesToWrite - bufferPos;
            if (length > available) {
                length = available;
            }
            //System.out.println("writing: length = " + length + "; bufferPos = " + bufferPos + "; offset = " + offset
            //        + "; buffer.length = " + audioBuffer.getLength() + "; buffer.offset = " + audioBuffer.getOffset());
            //System.out.println("AudioRenderer: before writing to line: available = " + available + "; length = "
            //        + length);
            int bytesWritten = line.write(audioBuffer, offset, length);
            //System.out.println("AudioRenderer: after else {writing to line: bytesWritten: " + bytesWritten);
            //System.out.println("written " + bytesWritten + " bytes to audio line");

            bufferPos += bytesWritten;
            if (bufferPos == bytesToWrite) {
                //System.err.println("DEBUG: Finished playing audio for frame "
                //        + frameQueue.peek().seqNum);
                CachedFrame frame = frameQueue.peek();
                System.out.println("TRACE: " + frame.seqNum + ": " + bytesWritten
                        + " bytes in audio buffer; video buffer eom?: " + frame.frame.isEndOfMedia());
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

    private void handleSetSpeed(SetSpeed message) {
        speed = message.newSpeed;
    }

    @Override
    protected void destruct() {
        // close sound channel
        if (line.isOpen()) {
            line.close();
        }
    }
}
