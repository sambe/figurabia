/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 04.07.2010
 */
package figurabia.ui.figuremapper;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

public interface ConnectionDrawer {

    /**
     * This method is called for drawing a line between two positions
     * 
     * @param g the graphics2D object
     * @param previous the previous position's point (can be null if at start of the line)
     * @param from the starting position's point for the line
     * @param to the ending position's point for the line
     * @param next the next position's point (can be null if at end of the line)
     */
    void draw(Graphics2D g, Point2D previous, Point2D from, Point2D to, Point2D next);
}
