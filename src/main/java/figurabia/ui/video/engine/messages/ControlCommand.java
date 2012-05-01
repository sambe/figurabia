/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 26.12.2010
 */
package figurabia.ui.video.engine.messages;

public class ControlCommand {

    public enum Command {
        /**
         * Command to start playback.
         */
        START,
        /**
         * Command to stop playback.
         */
        STOP,
        /**
         * Command to flush data in buffer
         */
        FLUSH,
        /**
         * Command to close underlying resource. No further commands are allowed and message handling stops.
         */
        CLOSE;
    }

    public final Command command;

    public ControlCommand(Command command) {
        this.command = command;
    }
}
