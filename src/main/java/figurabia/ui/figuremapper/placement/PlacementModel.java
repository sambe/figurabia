/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Aug 5, 2012
 */
package figurabia.ui.figuremapper.placement;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.Set;

import figurabia.domain.Figure;
import figurabia.domain.PuertoPosition;

public interface PlacementModel {

    Set<PuertoPosition> getAllPositions();

    Point2D getCoord(PuertoPosition pp);

    void setCoord(PuertoPosition pp, Point2D newCoord);

    void addFigure(Figure f);

    void removeFigure(Figure f);

    void recalculate(Dimension size);

    void startRelax();

    void stopRelax();

    void setPause(int millis);

    void setInitial(int millis);

    public interface PlacementChangeListener {
        void update();
    }

    void addPlacementChangeListener(PlacementChangeListener l);

    void removePlacementChangeListener(PlacementChangeListener l);
}
