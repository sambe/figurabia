/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 04.07.2010
 */
package figurabia.ui.figuremapper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.JComponent;

import edu.uci.ics.jung.algorithms.layout.KKLayout;
import figurabia.domain.Figure;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.framework.FigurabiaModel;
import figurabia.framework.FigureIndexListener;
import figurabia.framework.ViewSetListener;
import figurabia.ui.figuremapper.placement.JungPlacementModel;
import figurabia.ui.figuremapper.placement.PlacementModel;
import figurabia.ui.positionviewer.PositionPainter;

@SuppressWarnings("serial")
public class FigureMapScreen extends JComponent {

    private static final int WIDTH = 20;
    private static final double SCALE_STEP = 1.5;

    private FigurabiaModel figurabiaModel;
    private ConnectionDrawer connectionDrawer;

    private PlacementModel placementModel;
    private Collection<Figure> selectedFigures = Collections.emptySet();
    private boolean selectedFiguresChanged;

    private PuertoPosition selectedPosition;
    private double moveDiffX, moveDiffY;
    private Point originalPoint;
    private AffineTransform originalTransform = new AffineTransform();

    private static final Dimension LAYOUT_SIZE = new Dimension(1000, 700);
    private static final AffineTransform INITIAL_TRANSFORM = AffineTransform.getTranslateInstance(
            -LAYOUT_SIZE.width,
            -LAYOUT_SIZE.height);
    private AffineTransform transform = new AffineTransform(INITIAL_TRANSFORM);

    private Map<PuertoPosition, BufferedImage> positionImages = new WeakHashMap<PuertoPosition, BufferedImage>();
    double scaledWidth = WIDTH;

    public FigureMapScreen(FigurabiaModel fm) {
        figurabiaModel = fm;
        //placementModel = new StrategyPlacementModel();
        placementModel = new JungPlacementModel(KKLayout.class, 2.0);
        placementModel.setInitial(800);
        placementModel.setPause(500);
        setOpaque(true);
        connectionDrawer = new CurveConnectionDrawer();

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                Point eventPoint = event.getPoint();
                Point2D mp = inverseTransform(eventPoint);
                originalPoint = eventPoint;
                originalTransform.setTransform(transform);
                addMouseMotionListener(this);
                // select position by coordinates (CAUTION: if multiple are in range, the last wins!)
                for (PuertoPosition p : placementModel.getAllPositions()) {
                    Point2D coord = placementModel.getCoord(p);
                    if (coord.getX() + WIDTH > mp.getX() && coord.getX() - WIDTH < mp.getX()
                            && coord.getY() + WIDTH > mp.getY()
                            && coord.getY() - WIDTH < mp.getY()) {
                        selectedPosition = p;
                        moveDiffX = mp.getX() - coord.getX();
                        moveDiffY = mp.getY() - coord.getY();
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // reset on double click
                if (e.getClickCount() == 2) {
                    transform = new AffineTransform(INITIAL_TRANSFORM);
                    selectedPosition = null;
                    setScaledWidth(WIDTH);
                    paintImmediately(-50000, -50000, 100000, 100000);
                    return;
                }
                // only if one was selected
                if (selectedPosition != null) {
                    // set new coordinates of position
                    movePosition(e.getPoint());
                    selectedPosition = null;
                } else {
                    moveTransform(e.getPoint());
                }
                removeMouseMotionListener(this);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedPosition != null) {
                    movePosition(e.getPoint());
                } else {
                    moveTransform(e.getPoint());
                }
            }

            private void movePosition(Point mousePoint) {
                Point2D ptDst = inverseTransform(mousePoint);
                Point2D ptCorrectedDst = new Point2D.Double(ptDst.getX() - moveDiffX, ptDst.getY() - moveDiffY);
                placementModel.setCoord(selectedPosition, ptCorrectedDst);
                paintImmediately(-50000, -50000, 100000, 100000);
            }

            private void moveTransform(Point mousePoint) {
                Point2D ptOrig = inverseTransform(originalPoint);
                double oldX = ptOrig.getX();
                double oldY = ptOrig.getY();
                Point2D ptNew = inverseTransform(mousePoint);
                double newX = ptNew.getX();
                double newY = ptNew.getY();
                double dX = newX - oldX;
                double dY = newY - oldY;
                transform.setTransform(originalTransform);
                transform.translate(dX, dY);
                paintImmediately(-50000, -50000, 100000, 100000);
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
                double scale = clicks < 0 ? -SCALE_STEP * clicks : 1.0 / SCALE_STEP / clicks;
                Point mp = e.getPoint();
                Point2D ptFocus = inverseTransform(mp);
                // first, transform mouse location into center
                transform.translate(ptFocus.getX(), ptFocus.getY());
                // then, scale
                transform.scale(scale, scale);
                // last, transform center back into mouse location
                double afterScale = scale > 1.0 ? scale / SCALE_STEP : scale * SCALE_STEP;
                transform.translate(-ptFocus.getX() * afterScale, -ptFocus.getY() * afterScale);

                // update scaledWidth for caching position images and clear cache
                if (clicks > 0)
                    setScaledWidth(scaledWidth / SCALE_STEP);
                else
                    setScaledWidth(scaledWidth * SCALE_STEP);

                paintImmediately(-50000, -50000, 100000, 100000);
            }
        };
        addMouseWheelListener(mouseAdapter);
        addMouseListener(mouseAdapter);

        figurabiaModel.addViewSetListener(new ViewSetListener() {
            @Override
            public void update(ChangeType type, List<Figure> changed) {
                switch (type) {
                case ADDED:
                    for (Figure f : changed)
                        placementModel.addFigure(f);
                    selectedFiguresChanged = true;
                    break;
                case REMOVED:
                    for (Figure f : changed)
                        placementModel.removeFigure(f);
                    selectedFiguresChanged = true;
                    break;
                }
                if (isVisible())
                    refreshData();
            }
        });

        placementModel.addPlacementChangeListener(new PlacementModel.PlacementChangeListener() {
            @Override
            public void update() {
                repaint();
            }
        });

        figurabiaModel.addFigureIndexListener(new FigureIndexListener() {
            @Override
            public void update(Figure f, int position, boolean figureChanged) {
                repaint();
            }
        });
    }

    private void setScaledWidth(double scaledWidth) {
        this.scaledWidth = scaledWidth;
        positionImages.clear();
    }

    public void refreshData() {
        if (selectedFiguresChanged) {
            selectedFiguresChanged = false;
            selectedFigures = figurabiaModel.getViewSet();

            placementModel.stopRelax();
            placementModel.recalculate(LAYOUT_SIZE);

            placementModel.startRelax();
        }
    }

    private BufferedImage getPositionImage(PuertoPosition p) {
        PositionPainter pp = new PositionPainter();
        pp.setBaseOffset(PuertoOffset.getInitialOffset());
        pp.setOffset(PuertoOffset.getInitialOffset());

        if (!positionImages.containsKey(p)) {
            BufferedImage image = new BufferedImage((int) (2 * scaledWidth), (int) (2 * scaledWidth),
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            pp.setPosition(p);
            pp.paintCompactPosition(g, 0, 0, (int) (2 * scaledWidth), (int) (2 * scaledWidth));
            g.dispose();
            positionImages.put(p, image);
        }
        return positionImages.get(p);
    }

    /**
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g) {
        long startTime = System.nanoTime();
        // TODO this should not be needed to preserve the background color (but currently it is needed)
        System.out.println("Background color: " + ((Graphics2D) g).getBackground());
        ((Graphics2D) g).setBackground(new Color(21, 21, 21));
        // draw background
        g.clearRect(0, 0, getWidth(), getHeight());

        Graphics2D g2d = (Graphics2D) g;

        // move (0,0) into center
        g2d.translate(getWidth() / 2, getHeight() / 2);
        // apply transform (user's view)
        g2d.transform(transform);

        Figure selectedFigure = figurabiaModel.getCurrentFigure();
        PuertoPosition selectedPosition = PuertoPosition.getInitialPosition();
        if (selectedFigure != null && selectedFigure.getPositions().size() != 0) {
            int position = figurabiaModel.getCurrentFigureIndex();
            if (position != -1)
                selectedPosition = selectedFigure.getPositions().get(position);
        }

        // 1) draw all positions
        PositionPainter pp = new PositionPainter();
        pp.setBaseOffset(PuertoOffset.getInitialOffset());
        pp.setOffset(PuertoOffset.getInitialOffset());

        for (PuertoPosition p : placementModel.getAllPositions()) {
            Point2D pt = placementModel.getCoord(p);
            int x = (int) (pt.getX() - WIDTH);
            int y = (int) (pt.getY() - WIDTH);
            int w = (int) (2 * WIDTH);
            if (g.getClipBounds().intersects(x, y, w, w)) {
                if (scaledWidth < 1) {
                    // skip (to avoid exception, because too small)
                } else if (scaledWidth < 200) {
                    // prerender positions up to a certain size
                    BufferedImage image = getPositionImage(p);
                    g.drawImage(image, x, y, w,
                            w, null);
                } else {
                    // for larger sizes, render the real position (usually just one visible anyway, at this zoom level)
                    pp.setPosition(p);
                    pp.paintCompactPosition((Graphics2D) g, x, y, w, w);
                }
            }
            if (p.equals(selectedPosition)) {
                g.setColor(Color.LIGHT_GRAY);
                g.drawRect(x - 1, y - 1, w + 2, w + 2);
            }
        }

        // 2) draw figures as connections
        for (Figure f : selectedFigures) {
            g.setColor(f.getColor());
            List<PuertoPosition> positions = f.getPositions();
            Point2D previous = null;
            Point2D from = null;
            Point2D to = positions.size() > 0 ? placementModel.getCoord(positions.get(0)) : null;
            Point2D next = positions.size() > 1 ? placementModel.getCoord(positions.get(1)) : null;
            for (int i = 1; i < positions.size(); i++) {
                previous = from;
                from = to;
                to = next;
                next = positions.size() > i + 1 ? placementModel.getCoord(positions.get(i + 1)) : null;
                connectionDrawer.draw((Graphics2D) g, previous, from, to, next);
            }
        }
        long endTime = System.nanoTime();
        System.out.println("total time rendering figure map screen (w=" + scaledWidth + "): " + (endTime - startTime)
                / 1000000.0 + "ms");

    }
}
