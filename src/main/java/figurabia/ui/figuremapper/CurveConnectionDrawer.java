/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 15.08.2010
 */
package figurabia.ui.figuremapper;

import java.awt.Graphics2D;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import figurabia.ui.util.GeometryUtil;

public class CurveConnectionDrawer implements ConnectionDrawer {

    private CubicCurve2D cubicCurve = new CubicCurve2D.Double();

    private static final double ARROW_DEGREES = Math.PI / 8.0;
    private static final double ARROW_RADIUS = 10;
    private static final double ARROW_LENGTH = Math.cos(ARROW_DEGREES) * ARROW_RADIUS;

    @Override
    public void draw(Graphics2D g, Point2D previous, Point2D from, Point2D to, Point2D next) {
        // draw curve
        Point2D cp1;
        if (previous != null) {
            cp1 = getControlPoint(to, from, previous, false);//GeometryUtil.moveRelative(from, previous, -0.5);
        } else {
            cp1 = from;
        }
        Point2D cp2;
        if (next != null) {
            cp2 = getControlPoint(from, to, next, true);//GeometryUtil.moveRelative(to, next, -0.5);
        } else {
            cp2 = to;
        }

        Point2D ref;
        if (next != null) {
            ref = cp2;
        } else {
            ref = cp1;
        }
        double targetAngle = GeometryUtil.getAngle(to, ref);
        // draw small arrow
        Point2D arrowCorner1 = GeometryUtil.pointOnCircle(to, ARROW_RADIUS, targetAngle - ARROW_DEGREES);
        Point2D arrowCorner2 = GeometryUtil.pointOnCircle(to, ARROW_RADIUS, targetAngle + ARROW_DEGREES);
        Path2D path = new Path2D.Double();
        path.moveTo(to.getX(), to.getY());
        path.lineTo(arrowCorner1.getX(), arrowCorner1.getY());
        path.lineTo(arrowCorner2.getX(), arrowCorner2.getY());
        path.closePath();
        g.fill(path);

        Point2D modifiedTo = GeometryUtil.pointOnCircle(to, ARROW_LENGTH, targetAngle);
        Point2D modifiedCp2 = GeometryUtil.pointOnCircle(cp2, ARROW_LENGTH, targetAngle);
        cubicCurve.setCurve(from, cp1, modifiedCp2, modifiedTo);
        g.draw(cubicCurve);
    }

    private Point2D getControlPoint(Point2D near, Point2D point, Point2D far, boolean isTo) {
        // calculate angles
        double a1 = GeometryUtil.getAngle(point, near);
        double a2 = GeometryUtil.getAngle(far, point);

        // take mid angle on smaller side
        double midAngle = (a1 + a2) / 2.0;
        // if a1 and a2 are exactly opposite
        if (Math.abs(Math.abs(a2 - a1) - Math.PI) < 1e-9) {
            // FIXME does not work: needs opposite direction when (a1,a2) than when (a2,a1)
            //if (isTo && (point.getX() != near.getX() && point.getX() < near.getX() || point.getY() < near.getY())) {
            //    midAngle -= Math.PI;
            //}
            if (isTo) {
                midAngle -= Math.PI;
            }
        }
        // if the angle is on the larger angle side (shifted to smaller angle side)
        else if (Math.abs(a2 - a1) > Math.PI + 1e-9) {
            midAngle -= Math.PI;
        }

        // length = half of distance between points
        double length = (GeometryUtil.distance(point, near) - ARROW_LENGTH) / 2.0;

        return GeometryUtil.pointOnCircle(point, length, midAngle);
    }

    public static void main(String[] args) {
        CurveConnectionDrawer o = new CurveConnectionDrawer();
        Point2D point = new Point2D.Double(1.0, 2.0);
        for (double a = 0.0; a < 2 * Math.PI; a += Math.PI / 4.0) {
            Point2D near = GeometryUtil.pointOnCircle(point, 3.0, a);
            for (double b = 0.0; b < 2 * Math.PI; b += Math.PI / 4.0) {
                Point2D far = GeometryUtil.pointOnCircle(point, 2.0, b);
                Point2D cp = o.getControlPoint(near, point, far, false);
                System.out.println("a = " + (a / Math.PI / 2.0) + "; b = " + (b / Math.PI / 2.0) + "; cp angle = "
                        + (GeometryUtil.getAngle(point, cp) / Math.PI / 2.0));
            }
        }
    }
}
