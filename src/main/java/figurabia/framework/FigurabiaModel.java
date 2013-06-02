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
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.framework.ViewSetListener.ChangeType;

public class FigurabiaModel {

    private PuertoPosition currentPosition = PuertoPosition.getInitialPosition();
    private PuertoOffset currentOffset = PuertoOffset.getInitialOffset();
    private List<PositionListener> positionListeners = new LinkedList<PositionListener>();

    private Figure currentFigure;
    private int currentFigureIndex;
    private List<FigureIndexListener> figureListeners = new LinkedList<FigureIndexListener>();

    private Set<Figure> viewSet = new HashSet<Figure>();
    private List<ViewSetListener> viewSetListeners = new LinkedList<ViewSetListener>();

    public void addPositionListener(PositionListener l) {
        positionListeners.add(l);
    }

    public void removePositionListener(PositionListener l) {
        positionListeners.remove(l);
    }

    public void setCurrentPosition(PuertoPosition position) {
        setCurrentPosition(position, PuertoOffset.getInitialOffset());
    }

    public void setCurrentPosition(PuertoPosition position, PuertoOffset offset) {
        if (!position.equals(currentPosition)) {
            currentPosition = position;
            currentOffset = offset;
            notifyPositionListeners(position, offset);
        }
    }

    protected void notifyPositionListeners(PuertoPosition position, PuertoOffset offset) {
        for (PositionListener l : positionListeners) {
            try {
                l.update(position, offset);
            } catch (RuntimeException e) {
                System.err.println("ERROR: RuntimeException thrown from PositionListener with (" + position + ","
                        + offset + ")");
                e.printStackTrace();
            }
        }
    }

    public void addFigureIndexListener(FigureIndexListener l) {
        figureListeners.add(l);
    }

    public void removeFigureIndexListener(FigureIndexListener l) {
        figureListeners.remove(l);
    }

    protected void notifyFigureIndexListeners(Figure f, int index, boolean figureChanged) {
        for (FigureIndexListener l : figureListeners) {
            try {
                l.update(f, index, figureChanged);
            } catch (RuntimeException e) {
                System.err.println("ERROR: RuntimeException thrown from FigureIndexListener with (" + f + ","
                        + index + ")");
                e.printStackTrace();
            }
        }
    }

    public PuertoPosition getCurrentPosition() {
        return currentPosition;
    }

    public PuertoOffset getCurrentOffset() {
        return currentOffset;
    }

    public void setCurrentFigureIndex(int index) {
        setCurrentFigure(currentFigure, index);
    }

    public void setCurrentFigure(Figure f, int index) {
        if (f != currentFigure) {
            currentFigure = f;
            currentFigureIndex = index;
            notifyFigureIndexListeners(currentFigure, index, true);
            if (f != null & index != -1)
                setCurrentPosition(f.getPositions().get(index), f.getCombinedOffset(index));
        } else if (index != currentFigureIndex) {
            currentFigureIndex = index;
            notifyFigureIndexListeners(currentFigure, index, false);
            if (f != null & index != -1)
                setCurrentPosition(f.getPositions().get(index), f.getCombinedOffset(index));
        }
    }

    public Figure getCurrentFigure() {
        return currentFigure;
    }

    public int getCurrentFigureIndex() {
        return currentFigureIndex;
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
