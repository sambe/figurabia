/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 11.09.2011
 */
package figurabia.ui.video.engine.messages;

public class StatusResponse {

    public final long position;
    public final long positionMin;
    public final long positionMax;
    public final double speed;

    public StatusResponse(long position, long positionMin, long positionMax, double speed) {
        super();
        this.position = position;
        this.positionMin = positionMin;
        this.positionMax = positionMax;
        this.speed = speed;
    }
}
