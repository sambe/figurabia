/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 10.04.2011
 */
package figurabia.ui.video.engine;

import figurabia.ui.video.engine.actorframework.Actor;

public class VideoRenderer extends Actor {

    private final Actor recycler;

    public VideoRenderer(Actor errorHandler, Actor recycler) {
        super(errorHandler);
        this.recycler = recycler;
    }

    @Override
    protected void act(Object message) {
        // TODO Auto-generated method stub

    }

}
