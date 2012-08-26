/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Aug 12, 2012
 */
package figurabia.ui.figuremapper.placement;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import figurabia.domain.PuertoPosition;

public class StrategyPlacementModel extends AbstractPlacementModel {
    private Map<PuertoPosition, Point2D> coordinates;

    @Override
    public Set<PuertoPosition> getAllPositions() {
        return coordinates.keySet();
    }

    @Override
    public Point2D getCoord(PuertoPosition pp) {
        return coordinates.get(pp);
    }

    @Override
    public void setCoord(PuertoPosition pp, Point2D newCoord) {
        coordinates.put(pp, newCoord);
    }

    @Override
    public void recalculate(Dimension size) {
        coordinates = new HashMap<PuertoPosition, Point2D>();

        // set random coordinates for all positions
        //PlacementStrategy strategy = new RandomPlacement(getWidth(), getHeight(), 25);
        //PlacementStrategy strategy = new GraphBasedPlacement(getWidth(), getHeight(), 25);
        PlacementStrategy strategy = new JungLayoutPlacement(size);
        strategy.assignCoordinates(figures, coordinates);
    }
}
