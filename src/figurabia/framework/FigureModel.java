/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 08.03.2010
 */
package figurabia.framework;

import java.util.LinkedList;
import java.util.List;

import figurabia.domain.Figure;
import figurabia.framework.FigureListener.ChangeType;

public class FigureModel {

    private Figure currentFigure;
    private List<FigureListener> figureListeners = new LinkedList<FigureListener>();

    public void addFigureListener(FigureListener l) {
        figureListeners.add(l);
    }

    public void removeFigureListener(FigureListener l) {
        figureListeners.remove(l);
    }

    protected void notifyFigureListeners(ChangeType type, Figure f) {
        for (FigureListener l : figureListeners) {
            try {
                l.update(type, f);
            } catch (RuntimeException e) {
                System.err.println("ERROR: RuntimeException thrown from FigureListener with (" + type + "," + f + ")");
                e.printStackTrace();
            }
        }
    }

    public void setCurrentFigure(Figure f) {
        if (f != currentFigure) {
            currentFigure = f;
            notifyFigureListeners(ChangeType.FIGURE_CHANGED, currentFigure);
        }
    }

    public Figure getCurrentFigure() {
        return currentFigure;
    }
}
