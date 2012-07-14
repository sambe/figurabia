/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 04.07.2010
 */
package figurabia.ui.figuremapper;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.JComponent;

import figurabia.domain.Figure;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.io.FigureStore;
import figurabia.ui.figuremapper.placement.JungLayoutPlacement;
import figurabia.ui.figuremapper.placement.PlacementStrategy;
import figurabia.ui.positionviewer.PositionPainter;

@SuppressWarnings("serial")
public class FigureMapScreen extends JComponent {

    private static final double WIDTH = 20;

    private FigureStore figureStore;
    private ConnectionDrawer connectionDrawer;

    private Map<PuertoPosition, Point2D> coordinates;
    private Map<Figure, Color> colors;

    private PuertoPosition selectedPosition;
    private double moveDiffX, moveDiffY;
    private Point originalPoint;

    private AffineTransform transform = AffineTransform.getScaleInstance(0.25, 0.25);

    public FigureMapScreen(FigureStore fs) {
        figureStore = fs;
        setOpaque(true);
        /*connectionDrawer = new ConnectionDrawer() {
            @Override
            public void draw(Graphics2D g, Point2D previous, Point2D from, Point2D to, Point2D next) {
                // minimal implementation
                g.draw(new Line2D.Double(from.getX(), from.getY(), to.getX(), to.getY()));
            }
        };*/
        connectionDrawer = new CurveConnectionDrawer();

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                Point eventPoint = event.getPoint();
                Point2D mp = inverseTransform(eventPoint);
                originalPoint = eventPoint;
                // select position by coordinates (CAUTION: if multiple are in range, the last wins!)
                for (Entry<PuertoPosition, Point2D> e : coordinates.entrySet()) {
                    Point2D pp = e.getValue();
                    if (pp.getX() + WIDTH > mp.getX() && pp.getX() - WIDTH < mp.getX() && pp.getY() + WIDTH > mp.getY()
                            && pp.getY() - WIDTH < mp.getY()) {
                        selectedPosition = e.getKey();
                        moveDiffX = mp.getX() - pp.getX();
                        moveDiffY = mp.getY() - pp.getY();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // reset on double click
                if (e.getClickCount() == 2) {
                    transform = new AffineTransform();
                    selectedPosition = null;
                    paintImmediately(-50000, -50000, 100000, 100000);
                    return;
                }
                // only if one was selected
                if (selectedPosition != null) {
                    // set new coordinates of position
                    Point mp = e.getPoint();
                    Point2D ptDst = inverseTransform(mp);
                    Point2D ptCorrectedDst = new Point2D.Double(ptDst.getX() - moveDiffX, ptDst.getY() - moveDiffY);
                    coordinates.put(selectedPosition, ptCorrectedDst);
                    selectedPosition = null;
                    paintImmediately(-50000, -50000, 100000, 100000);
                } else {
                    Point2D ptOrig = inverseTransform(originalPoint);
                    double oldX = ptOrig.getX();
                    double oldY = ptOrig.getY();
                    Point mp = e.getPoint();
                    Point2D ptNew = inverseTransform(mp);
                    double newX = ptNew.getX();
                    double newY = ptNew.getY();
                    double dX = newX - oldX;
                    double dY = newY - oldY;
                    transform.translate(dX, dY);
                    paintImmediately(-50000, -50000, 100000, 100000);
                }
            }

            private Point2D inverseTransform(Point src) {
                Point2D ptSrc = new Point2D.Double(src.x - getWidth() / 2, src.y - getHeight() / 2);
                Point2D ptDst = new Point2D.Double();
                try {
                    transform.inverseTransform(ptSrc, ptDst);
                } catch (NoninvertibleTransformException ex) {
                    throw new IllegalStateException("should never happen", ex);
                }
                //System.out.println("DEBUG: position original: " + ptSrc + "; position destination: " + ptDst);
                return ptDst;
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int clicks = e.getWheelRotation();
                double scale = clicks < 0 ? -2 * clicks : 0.5 / clicks;
                Point mp = e.getPoint();
                Point2D ptFocus = inverseTransform(mp);
                // first, transform mouse location into center
                transform.translate(ptFocus.getX(), ptFocus.getY());
                // then, scale
                transform.scale(scale, scale);
                // last, transform center back into mouse location
                double afterScale = scale > 1.0 ? scale / 2.0 : scale * 2.0;
                transform.translate(-ptFocus.getX() * afterScale, -ptFocus.getY() * afterScale);

                paintImmediately(-50000, -50000, 100000, 100000);
            }
        };
        addMouseWheelListener(mouseAdapter);
        addMouseListener(mouseAdapter);
    }

    public void refreshData() {
        Collection<Figure> allFigures = figureStore.getAllActiveFigures();
        coordinates = new HashMap<PuertoPosition, Point2D>();
        colors = new HashMap<Figure, Color>();

        // set random coordinates for all positions
        //PlacementStrategy strategy = new RandomPlacement(getWidth(), getHeight(), 25);
        //PlacementStrategy strategy = new GraphBasedPlacement(getWidth(), getHeight(), 25);
        PlacementStrategy strategy = new JungLayoutPlacement(getSize());
        strategy.assignCoordinates(allFigures, coordinates);

        // set a color for each figure
        Random rand = new Random(1);
        for (Figure f : allFigures) {
            colors.put(f, new Color(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256)));
        }
    }

    /**
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g) {
        // draw background
        g.clearRect(0, 0, getWidth(), getHeight());

        Graphics2D g2d = (Graphics2D) g;

        // move (0,0) into center
        g2d.translate(getWidth() / 2, getHeight() / 2);
        // apply transform (user's view)
        g2d.transform(transform);

        // 1) draw all positions
        PositionPainter pp = new PositionPainter();
        pp.setBaseOffset(PuertoOffset.getInitialOffset());
        pp.setOffset(PuertoOffset.getInitialOffset());

        for (Map.Entry<PuertoPosition, Point2D> e : coordinates.entrySet()) {
            pp.setPosition(e.getKey());
            Point2D p = e.getValue();
            pp.paintCompactPosition((Graphics2D) g, (int) (p.getX() - WIDTH), (int) (p.getY() - WIDTH),
                    (int) (2 * WIDTH),
                    (int) (2 * WIDTH));
        }

        // 2) draw all figures as connections
        for (Map.Entry<Figure, Color> e : colors.entrySet()) {
            g.setColor(e.getValue());
            List<PuertoPosition> positions = e.getKey().getPositions();
            Point2D previous = null;
            Point2D from = null;
            Point2D to = positions.size() > 0 ? coordinates.get(positions.get(0)) : null;
            Point2D next = positions.size() > 1 ? coordinates.get(positions.get(1)) : null;
            for (int i = 1; i < positions.size(); i++) {
                previous = from;
                from = to;
                to = next;
                next = positions.size() > i + 1 ? coordinates.get(positions.get(i + 1)) : null;
                connectionDrawer.draw((Graphics2D) g, previous, from, to, next);
            }
        }
    }
}
