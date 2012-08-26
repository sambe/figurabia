/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Aug 12, 2012
 */
package figurabia.ui.figuremapper.placement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import figurabia.domain.Figure;

public abstract class AbstractPlacementModel implements PlacementModel {

    protected Set<Figure> figures = new HashSet<Figure>();
    private List<PlacementChangeListener> placementChangeListeners = new ArrayList<JungPlacementModel.PlacementChangeListener>();

    public AbstractPlacementModel() {
        super();
    }

    @Override
    public void addPlacementChangeListener(PlacementChangeListener l) {
        placementChangeListeners.add(l);
    }

    @Override
    public void removePlacementChangeListener(PlacementChangeListener l) {
        placementChangeListeners.remove(l);
    }

    protected void notifyPlacementChangeListeners() {
        for (PlacementChangeListener l : placementChangeListeners) {
            l.update();
        }
    }

    @Override
    public void addFigure(Figure f) {
        figures.add(f);
    }

    @Override
    public void removeFigure(Figure f) {
        figures.remove(f);
    }

    @Override
    public void startRelax() {
        // default implementation
    }

    @Override
    public void stopRelax() {
        // default implementation
    }

    @Override
    public void setPause(int millis) {
        // default implementation
    }

    @Override
    public void setInitial(int millis) {
        // default implementation
    }
}
