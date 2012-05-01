/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 20.03.2011
 */
package figurabia.ui.video.engine.actorframework;

/**
 * Represents the capability of getting a message sent
 * 
 * @author Samuel Berner
 */
public interface MessageSendable {
    void send(Object message);
}
