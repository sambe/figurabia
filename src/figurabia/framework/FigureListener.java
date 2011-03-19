/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 26.08.2009
 */
package figurabia.framework;

import figurabia.domain.Figure;

public interface FigureListener {

    public enum ChangeType {
        FIGURE_ADDED, FIGURE_REMOVED, FIGURE_CHANGED;
    }

    /**
     * The listener method.
     * 
     * @param type the change type
     * @param figure the concerning figure
     */
    void update(ChangeType type, Figure figure);
}
