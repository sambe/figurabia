/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 11.09.2011
 */
package figurabia.ui.video.engine;

import java.io.File;

import javax.sound.sampled.AudioFormat;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import com.xuggle.ferry.JNIMemoryManager;
import com.xuggle.ferry.JNIMemoryManager.MemoryModel;

import figurabia.ui.util.SimplePanelFrame;
import figurabia.ui.video.access.VideoFormat;
import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.actorframework.MessageSendable;
import figurabia.ui.video.engine.actorframework.ObjectReceiver;
import figurabia.ui.video.engine.actorframework.RegisterForUpdates;
import figurabia.ui.video.engine.messages.CachedFrame;
import figurabia.ui.video.engine.messages.ControlCommand;
import figurabia.ui.video.engine.messages.ControlCommand.Command;
import figurabia.ui.video.engine.messages.CurrentScreen;
import figurabia.ui.video.engine.messages.FrameRequest;
import figurabia.ui.video.engine.messages.MediaError;
import figurabia.ui.video.engine.messages.MediaInfoRequest;
import figurabia.ui.video.engine.messages.MediaInfoResponse;
import figurabia.ui.video.engine.messages.NewVideo;
import figurabia.ui.video.engine.messages.PositionUpdate;
import figurabia.ui.video.engine.messages.SetPosition;
import figurabia.ui.video.engine.messages.StatusRequest;
import figurabia.ui.video.engine.messages.StatusResponse;
import figurabia.ui.video.engine.ui.ControlBar;
import figurabia.ui.video.engine.ui.VideoScreen;

public class MediaPlayer {

    private Actor errorHandler = new Actor(null, -1) {
        @Override
        protected void act(Object message) {
            if (message instanceof MediaError) {
                MediaError me = (MediaError) message;
                System.err.println("A media error occured: " + me.message);
                me.exception.printStackTrace();
            } else {
                System.err.println("A unknown type of error occured: " + message);
            }
        }
    };
    private Controller controller = new Controller(errorHandler);
    private VideoRenderer videoRenderer;
    private MediaInfoResponse mediaInfo;

    public MediaPlayer() {
        errorHandler.start();
        controller.start();
        videoRenderer = controller.getVideoRenderer();
    }

    public VideoScreen createScreen() {
        VideoScreen screen = new VideoScreen(videoRenderer);
        return screen;
    }

    public ControlBar createControlBar() {
        ControlBar controlBar = new ControlBar(controller);
        return controlBar;
    }

    public void setActiveScreen(VideoScreen screen) {
        videoRenderer.send(new CurrentScreen(screen));
    }

    public void openVideo(File file) {
        openVideo(new NewVideo(file));
    }

    public void openVideo(File file, long initialPosition) {
        openVideo(new NewVideo(file, initialPosition));
    }

    public void openVideo(File file, long timerMin, long timerMax) {
        openVideo(new NewVideo(file, timerMin, timerMax));
    }

    public void openVideo(File file, long initialPosition, long timerMin, long timerMax) {
        openVideo(new NewVideo(file, initialPosition, timerMin, timerMax));
    }

    private void openVideo(NewVideo message) {
        controller.send(message);
        ObjectReceiver r = new ObjectReceiver();
        controller.send(new MediaInfoRequest(r));
        mediaInfo = (MediaInfoResponse) r.waitForMessage();
    }

    public AudioFormat getAudioFormat() {
        return mediaInfo.audioFormat;
    }

    public VideoFormat getVideoFormat() {
        return mediaInfo.videoFormat;
    }

    public long getVideoDuration() {
        return mediaInfo.duration;
    }

    public long getPosition() {
        ObjectReceiver receiver = new ObjectReceiver();
        controller.send(new StatusRequest(receiver));
        StatusResponse response = (StatusResponse) receiver.waitForMessage();
        return response.position;
    }

    public void setPosition(long position) {
        controller.send(new SetPosition(position));
    }

    public void setPositionAnimated(long position) {
        controller.send(new SetPosition(position, true));
    }

    public void start() {
        controller.send(new ControlCommand(Command.START));
    }

    public void stop() {
        controller.send(new ControlCommand(Command.STOP));
    }

    public void addPositionListener(final PositionListener listener) {
        controller.send(new RegisterForUpdates(PositionUpdate.class, new MessageSendable() {
            @Override
            public void send(final Object message) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        listener.receive((PositionUpdate) message);
                    }
                });
            }
        }));
    }

    public void restrictPositionRange(Long min, Long max) {
        controller.send(new StatusRequest(min, max, null));
    }

    /**
     * Returns a frame (CachedFrame).
     * 
     * Very important: never forget to recycle() the returned frame (or you'll block a part of the cache forever)
     * 
     * @param seqNum
     * @return
     */
    public CachedFrame getFrame(long seqNum) {
        ObjectReceiver r = new ObjectReceiver();
        controller.send(new FrameRequest(seqNum, 1, false, r));
        return (CachedFrame) r.waitForMessage();
    }

    public static void main(String[] args) {
        JNIMemoryManager.setMemoryModel(MemoryModel.NATIVE_BUFFERS);
        MediaPlayer mediaPlayer = new MediaPlayer();

        VideoScreen screen = mediaPlayer.createScreen();
        ControlBar controlBar = mediaPlayer.createControlBar();
        mediaPlayer.setActiveScreen(screen);

        //mediaPlayer.openVideo(new File("/home/sberner/Desktop/10-21.04.09.flv"));
        mediaPlayer.openVideo(new File("/home/sberner/media/salsavids/m2/MOV00356.MP4"));

        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 0,gap 0", "[fill]", "[fill][20, fill]"));
        panel.add(screen, "wrap,push");
        panel.add(controlBar, "");
        int width = 640;
        int height = 480 + 20;
        SimplePanelFrame frame = new SimplePanelFrame(panel, width + 20, height + 20 + 65);
    }
}
