/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 26.12.2010
 */
package figurabia.ui.video.engine.messages;

/**
 * This message simply returns an object for recycling.
 */
public class RecyclingBag {

    public final Object object;

    public RecyclingBag(Object object) {
        super();
        this.object = object;
    }
}
