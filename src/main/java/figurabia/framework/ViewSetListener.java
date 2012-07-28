/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 21, 2012
 */
package figurabia.framework;

import java.util.List;

import figurabia.domain.Figure;

public interface ViewSetListener {

    enum ChangeType {
        ADDED, REMOVED
    };

    public void update(ChangeType type, List<Figure> changed);
}
