/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 02.06.2011
 */
package figurabia.ui.video.experiment;

import java.io.File;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import figurabia.ui.util.SimplePanelFrame;
import figurabia.ui.video.engine.Controller;
import figurabia.ui.video.engine.VideoRenderer;
import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.messages.CurrentScreen;
import figurabia.ui.video.engine.messages.MediaError;
import figurabia.ui.video.engine.messages.NewVideo;
import figurabia.ui.video.engine.messages.SetPosition;
import figurabia.ui.video.engine.messages.StatusRequest;
import figurabia.ui.video.engine.ui.ControlBar;
import figurabia.ui.video.engine.ui.VideoScreen;

public class ActorMoviePlayer2 {

    public static void main(String[] args) throws Exception {
        Actor errorHandler = new Actor(null) {
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
        Controller controller = new Controller(errorHandler);

        errorHandler.start();
        controller.start();

        VideoRenderer videoRenderer = controller.getVideoRenderer();
        VideoScreen screen = createVideoScreen(controller, videoRenderer);
        VideoScreen screen2 = createVideoScreen(controller, videoRenderer);

        videoRenderer.send(new CurrentScreen(screen));

        controller.send(new NewVideo(new File("/home/sberner/Desktop/10-21.04.09.flv")));
        controller.send(new SetPosition(0));

        Thread.sleep(20000);

        controller.send(new StatusRequest(5000L, 15000L, null));
        videoRenderer.send(new CurrentScreen(screen2));

        //controller.send(new ControlCommand(Command.START));
    }

    private static VideoScreen createVideoScreen(Controller controller, VideoRenderer videoRenderer) {
        VideoScreen screen = new VideoScreen(videoRenderer);
        ControlBar controlBar = new ControlBar(controller);
        JPanel panel = new JPanel();
        panel.setLayout(new MigLayout("ins 0,gap 0", "[fill]", "[fill][20, fill]"));
        panel.add(screen, "wrap,push");
        panel.add(controlBar, "");
        int width = 640;
        int height = 480 + 20;
        SimplePanelFrame frame = new SimplePanelFrame(panel, width + 20, height + 20 + 65);
        return screen;
    }
}
