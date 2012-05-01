/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 14.09.2011
 */
package figurabia.ui.video.engine;

import figurabia.ui.video.engine.messages.PositionUpdate;

public interface PositionListener {

    void receive(PositionUpdate update);
}
