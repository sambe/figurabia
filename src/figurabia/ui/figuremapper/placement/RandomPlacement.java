/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 18.07.2010
 */
package figurabia.ui.figuremapper.placement;

import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

import figurabia.domain.Figure;
import figurabia.domain.PuertoPosition;

public class RandomPlacement implements PlacementStrategy {

    private double maxX;
    private double maxY;
    private double margin;

    public RandomPlacement(int width, int height, int margin) {
        this.maxX = width - 2 * margin;
        this.maxY = height - 2 * margin;
        this.margin = margin;
    }

    @Override
    public void assignCoordinates(Collection<Figure> allFigures, Map<PuertoPosition, Point2D> coordinates) {
        Random rand = new Random(1);

        // set random coordinates for all positions & a color for each figure
        for (Figure f : allFigures) {
            for (PuertoPosition p : f.getPositions()) {
                if (!coordinates.containsKey(p)) {
                    double randomX = margin + rand.nextDouble() * maxX;
                    double randomY = margin + rand.nextDouble() * maxY;
                    coordinates.put(p, new Point2D.Double(randomX, randomY));
                }
            }
        }
    }

}
