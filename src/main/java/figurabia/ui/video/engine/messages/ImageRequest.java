/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 12.06.2011
 */
package figurabia.ui.video.engine.messages;

import figurabia.ui.video.engine.actorframework.MessageSendable;

public class ImageRequest extends ResponseRequest {

    public ImageRequest(MessageSendable responseTo) {
        super(responseTo);
    }
}
