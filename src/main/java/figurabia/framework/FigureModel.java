/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 08.03.2010
 */
package figurabia.framework;

import java.util.LinkedList;
import java.util.List;

import figurabia.domain.Figure;

public class FigureModel {

    private Figure currentFigure;
    private int currentPosition;
    private List<FigurePositionListener> figureListeners = new LinkedList<FigurePositionListener>();

    public void addFigurePositionListener(FigurePositionListener l) {
        figureListeners.add(l);
    }

    public void removeFigurePositionListener(FigureListener l) {
        figureListeners.remove(l);
    }

    protected void notifyFigurePositionListeners(Figure f, int position) {
        for (FigurePositionListener l : figureListeners) {
            try {
                l.update(f, position);
            } catch (RuntimeException e) {
                System.err.println("ERROR: RuntimeException thrown from FigurePositionListener with (" + f + ","
                        + position + ")");
                e.printStackTrace();
            }
        }
    }

    public void setCurrentPosition(int position) {
        if (position != currentPosition) {
            currentPosition = position;
        }
    }

    public void setCurrentFigure(Figure f, int position) {
        if (position != currentPosition) {
            currentPosition = position;
        }
        if (f != currentFigure) {
            currentFigure = f;
            notifyFigurePositionListeners(currentFigure, position);
        }
    }

    public Figure getCurrentFigure() {
        return currentFigure;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }
}
