/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 01.08.2011
 */
package figurabia.ui.video.engine.messages;

import figurabia.ui.video.engine.Controller;

public class StateUpdate {
    public final Controller.State state;

    public StateUpdate(Controller.State state) {
        this.state = state;
    }
}
