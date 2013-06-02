/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 19.10.2009
 */
package figurabia.ui.framework;

import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;

/**
 * Allows to listen for changes in the active position.
 * 
 * @author Samuel Berner
 */
public interface PositionChangeListener {

    /**
     * Passes the newly active position
     * 
     * @param p the position
     * @param combinedOffset the current offset (cumulated)
     * @param offset the offset change
     */
    void positionActive(PuertoPosition p, PuertoOffset combinedOffset, PuertoOffset offset);
}
