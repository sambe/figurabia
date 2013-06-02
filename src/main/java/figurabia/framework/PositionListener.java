/*
 * Copyright (c) 2013 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 2, 2013
 */
package figurabia.framework;

import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;

public interface PositionListener {

    void update(PuertoPosition position, PuertoOffset offset);
}
