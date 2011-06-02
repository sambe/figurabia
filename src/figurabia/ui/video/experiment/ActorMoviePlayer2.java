/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 02.06.2011
 */
package figurabia.ui.video.experiment;

import java.io.File;

import figurabia.ui.util.SimplePanelFrame;
import figurabia.ui.video.engine.Controller;
import figurabia.ui.video.engine.VideoRenderer;
import figurabia.ui.video.engine.VideoScreen;
import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.messages.ControlCommand;
import figurabia.ui.video.engine.messages.CurrentScreen;
import figurabia.ui.video.engine.messages.MediaError;
import figurabia.ui.video.engine.messages.NewVideo;
import figurabia.ui.video.engine.messages.SetPosition;
import figurabia.ui.video.engine.messages.ControlCommand.Command;

public class ActorMoviePlayer2 {

    public static void main(String[] args) {
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
        VideoScreen screen = new VideoScreen(videoRenderer);
        int width = 640;
        int height = 480;
        SimplePanelFrame frame = new SimplePanelFrame(screen, width + 20, height + 20 + 65);

        videoRenderer.send(new CurrentScreen(screen));

        controller.send(new NewVideo(new File("/home/sberner/Desktop/10-21.04.09.flv"), 0));
        controller.send(new SetPosition(0));

        controller.send(new ControlCommand(Command.START));
    }
}
