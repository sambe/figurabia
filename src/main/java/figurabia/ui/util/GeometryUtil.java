/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 15.08.2010
 */
package figurabia.ui.util;

import java.awt.geom.Point2D;

public class GeometryUtil {

    public static Point2D add(Point2D p1, Point2D p2) {
        return new Point2D.Double(p1.getX() + p2.getX(), p1.getY() + p2.getY());
    }

    public static Point2D scale(Point2D p, double scale) {
        return new Point2D.Double(p.getX() * scale, p.getY() * scale);
    }

    public static double dotProduct(Point2D p1, Point2D p2) {
        return p1.getX() * p2.getY() - p1.getY() * p2.getX();
    }

    public static double dotProduct(Point2D p1, Point2D p2, Point2D ref) {
        return dotProduct(new Point2D.Double(p1.getX() - ref.getX(), p1.getY() - ref.getY()), new Point2D.Double(
                p2.getX() - ref.getX(), p2.getY() - ref.getY()));
    }

    public static double getAngle(Point2D center, Point2D point) {
        return Math.atan2(-(point.getY() - center.getY()), point.getX() - center.getX());
    }

    public static Point2D pointOnCircle(Point2D center, double radius, double angle) {
        double dy = -radius * Math.sin(angle);
        double dx = radius * Math.cos(angle);
        return new Point2D.Double(center.getX() + dx, center.getY() + dy);
    }

    public static Point2D scaledDiff(Point2D from, Point2D to, double scale) {
        return new Point2D.Double(scale * (to.getX() - from.getX()), scale * (to.getY() - from.getY()));
    }

    public static Point2D moveRelative(Point2D p, Point2D ref, double scale) {
        return new Point2D.Double(p.getX() + scale * (ref.getX() - p.getX()),
                p.getY() + scale * (ref.getY() - p.getY()));
    }

    public static double squaredDistance(Point2D p1, Point2D p2) {
        return Point2D.distanceSq(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public static double distance(Point2D p1, Point2D p2) {
        return Point2D.distance(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }
}
