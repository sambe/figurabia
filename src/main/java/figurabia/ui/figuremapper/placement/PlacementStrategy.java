/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 18.07.2010
 */
package figurabia.ui.figuremapper.placement;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Map;

import figurabia.domain.Figure;
import figurabia.domain.PuertoPosition;

public interface PlacementStrategy {

    /**
     * Offers a strategy for assigning coordinates to all figures.
     * 
     * @param allFigures all figures
     * @param coordinates the map to populate with coordinates
     */
    void assignCoordinates(Collection<Figure> allFigures, Map<PuertoPosition, Point2D> coordinates);
}
