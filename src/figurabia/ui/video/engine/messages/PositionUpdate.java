/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.07.2011
 */
package figurabia.ui.video.engine.messages;

public class PositionUpdate {

    public final long position;
    public final long startPosition;
    public final long endPosition;

    public PositionUpdate(long position, long startPosition, long endPosition) {
        this.position = position;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
    }
}
