/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Apr 30, 2012
 */
package figurabia.ui.video.experiment;

import java.io.File;
import java.lang.reflect.Field;

import javax.sound.sampled.AudioFormat;

import figurabia.ui.video.access.AudioBuffer;
import figurabia.ui.video.access.MediaFrame;
import figurabia.ui.video.access.XugglerMediaInputStream;
import figurabia.ui.video.engine.AudioRenderer;
import figurabia.ui.video.engine.FrameCache;
import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.messages.CachedFrame;
import figurabia.ui.video.engine.messages.ControlCommand;
import figurabia.ui.video.engine.messages.ControlCommand.Command;
import figurabia.ui.video.engine.messages.MediaError;

public class AudioPulseAudioCrasher {

    public static void main(String[] args) throws Exception {
        Actor errorHandler = new Actor(null, -1) {
            @Override
            protected void act(Object message) {
                if (message instanceof MediaError) {
                    MediaError error = (MediaError) message;
                    System.err.println("ERROR: " + error.source + ": " + error.message);
                    if (error.exception != null)
                        error.exception.printStackTrace();
                } else {
                    System.err.println("ERROR: " + message);
                }
            }
        };
        FrameCache frameCacheStub = new FrameCache(errorHandler, null);
        AudioFormat audioFormat = new AudioFormat(22050, 16, 1, true, false);
        AudioRenderer renderer = new AudioRenderer(errorHandler, audioFormat, null);
        errorHandler.start();
        renderer.start();

        // NOTE: It crashes always if and only if LD_LIBRARY_PATH is set to the xuggle lib directory (other empty directory: no crash)
        //       i.e. there must be some binary incompatibility between precompiled Xuggle and /usr/lib/i386-linux-gnu/libpulse.so.0
        // REMEMBER: there is also the workaround using padsp
        XugglerMediaInputStream is = new XugglerMediaInputStream(new File(
                "/home/sberner/media/salsavids/m2/MOV00003.MP4"));
        is.setPosition(0);
        MediaFrame mf = is.createFrame();
        is.readFrame(mf);
        is.readFrame(mf);
        is.readFrame(mf);
        is.readFrame(mf);
        is.readFrame(mf);
        is.readFrame(mf);
        is.readFrame(mf);
        is.readFrame(mf);

        System.out.println("LD_LIBRARY_PATH = " + System.getenv("LD_LIBRARY_PATH") + "   // complete line visible");

        renderer.send(new ControlCommand(Command.START));

        Field f = AudioBuffer.class.getDeclaredField("audioData");
        f.setAccessible(true);

        CachedFrame[] cachedFrames = new CachedFrame[50];
        for (int i = 0; i < cachedFrames.length; i++) {
            cachedFrames[i] = new CachedFrame(0, frameCacheStub);
            AudioBuffer audioBuffer = new AudioBuffer();
            f.set(audioBuffer, new byte[4096]);
            byte[] data = audioBuffer.getAudioData();
            for (int j = 0; j < audioBuffer.getSize() / 2; j++) {
                short value = (short) (Short.MAX_VALUE / 4 * Math.sin(j / (4 + 2 * Math.pow(1.05, i))));
                data[j * 2] = (byte) (value % 256);
                data[j * 2 + 1] = (byte) (value / 256);
            }

            cachedFrames[i].frame = new MediaFrame(audioBuffer, null);
        }

        for (int i = 0; i < cachedFrames.length; i++) {
            renderer.send(cachedFrames[i]);
        }
    }
}
