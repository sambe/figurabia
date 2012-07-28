/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 08.03.2010
 */
package figurabia.framework;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import figurabia.domain.Figure;
import figurabia.framework.ViewSetListener.ChangeType;

public class FigureModel {

    private Figure currentFigure;
    private int currentPosition;
    private List<FigurePositionListener> figureListeners = new LinkedList<FigurePositionListener>();

    private Set<Figure> viewSet = new HashSet<Figure>();
    private List<ViewSetListener> viewSetListeners = new LinkedList<ViewSetListener>();

    public void addFigurePositionListener(FigurePositionListener l) {
        figureListeners.add(l);
    }

    public void removeFigurePositionListener(FigurePositionListener l) {
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

    public Set<Figure> getViewSet() {
        return Collections.unmodifiableSet(viewSet);
    }

    public void addToViewSet(Figure f) {
        viewSet.add(f);
        notifyViewSetListeners(ChangeType.ADDED, Arrays.asList(f));
    }

    public void addToViewSet(List<Figure> list) {
        viewSet.addAll(list);
        notifyViewSetListeners(ChangeType.ADDED, list);
    }

    public void removeFromViewSet(Figure f) {
        viewSet.remove(f);
        notifyViewSetListeners(ChangeType.REMOVED, Arrays.asList(f));
    }

    public void removeFromViewSet(List<Figure> list) {
        viewSet.removeAll(list);
        notifyViewSetListeners(ChangeType.REMOVED, list);
    }

    public void addViewSetListener(ViewSetListener l) {
        viewSetListeners.add(l);
    }

    public void removeViewSetListener(ViewSetListener l) {
        viewSetListeners.remove(l);
    }

    protected void notifyViewSetListeners(ChangeType type, List<Figure> changed) {
        for (ViewSetListener l : viewSetListeners) {
            try {
                l.update(type, changed);
            } catch (RuntimeException e) {
                System.err.println("ERROR: RuntimeException thrown from ViewSetListener with (" + type + ","
                        + changed + ")");
                e.printStackTrace();
            }
        }
    }
}
