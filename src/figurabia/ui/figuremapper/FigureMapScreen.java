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
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import javax.swing.JComponent;

import figurabia.domain.Figure;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.framework.PersistenceProvider;
import figurabia.ui.figuremapper.placement.GraphBasedPlacement;
import figurabia.ui.figuremapper.placement.PlacementStrategy;
import figurabia.ui.positionviewer.PositionPainter;

@SuppressWarnings("serial")
public class FigureMapScreen extends JComponent {

    private static final double WIDTH = 20;

    private PersistenceProvider persistenceProvider;
    private ConnectionDrawer connectionDrawer;

    private Map<PuertoPosition, Point2D> coordinates;
    private Map<Figure, Color> colors;

    private PuertoPosition selectedPosition;

    public FigureMapScreen(PersistenceProvider pp) {
        persistenceProvider = pp;
        setOpaque(true);
        /*connectionDrawer = new ConnectionDrawer() {
            @Override
            public void draw(Graphics2D g, Point2D previous, Point2D from, Point2D to, Point2D next) {
                // minimal implementation
                g.draw(new Line2D.Double(from.getX(), from.getY(), to.getX(), to.getY()));
            }
        };*/
        connectionDrawer = new CurveConnectionDrawer();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                Point mp = event.getPoint();
                // select position by coordinates (CAUTION: if multiple are in range, the last wins!)
                for (Entry<PuertoPosition, Point2D> e : coordinates.entrySet()) {
                    Point2D pp = e.getValue();
                    if (pp.getX() + WIDTH > mp.x && pp.getX() - WIDTH < mp.x && pp.getY() + WIDTH > mp.y
                            && pp.getY() - WIDTH < mp.y) {
                        selectedPosition = e.getKey();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // only if one was selected
                if (selectedPosition != null) {
                    // set new coordinates of position
                    Point mp = e.getPoint();
                    coordinates.put(selectedPosition, new Point2D.Double(mp.x, mp.y));
                    selectedPosition = null;
                    paintImmediately(-50000, -50000, 100000, 100000);
                }
            }
        });
    }

    public void refreshData() {
        Collection<Figure> allFigures = persistenceProvider.getAllActiveFigures();
        coordinates = new HashMap<PuertoPosition, Point2D>();
        colors = new HashMap<Figure, Color>();

        // set random coordinates for all positions
        //PlacementStrategy strategy = new RandomPlacement(getWidth(), getHeight(), 25);
        PlacementStrategy strategy = new GraphBasedPlacement(getWidth(), getHeight(), 25);
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
