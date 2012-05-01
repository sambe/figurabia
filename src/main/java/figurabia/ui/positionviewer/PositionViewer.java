/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 10.08.2009
 */
package figurabia.ui.positionviewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.domain.PuertoPosition.ArmWrapped;
import figurabia.domain.PuertoPosition.HandJoint;
import figurabia.domain.PuertoPosition.ManDir;
import figurabia.domain.util.PositionMutator;
import figurabia.ui.framework.PositionListener;
import figurabia.ui.positionviewer.PositionPainter.Selection;
import figurabia.ui.util.SimplePanelFrame;

@SuppressWarnings("serial")
public class PositionViewer extends JComponent {

    private static final Color OVERLAY_COLOR = new Color(191, 191, 191, 63);
    private static final Color OVERLAY_SELECTED_COLOR = new Color(255, 204, 0, 127);

    private static final long MIN_CLICK_INTERVAL = 250;

    private boolean editable = false;
    private boolean beatChangeable = false;
    private Rectangle beatBounds = new Rectangle(17, -20, 3, 4);
    private boolean showManCoords = false;
    private boolean showLadyCoords = false;

    private enum Mode {
        DEFAULT, DRAGGING;
    }

    private Mode mode = Mode.DEFAULT;
    private boolean firstClick = false;
    private long mousePressedTime;
    private int hoverX = 0;
    private int hoverY = 0;
    private PositionPainter painter = new PositionPainter();
    private List<PositionListener> positionListeners = new ArrayList<PositionListener>();
    private int suppressingUpdates = 0;
    private int suppressedUpdates = 0;

    public PositionViewer() {
        PositionViewerMouseListener listener = new PositionViewerMouseListener();
        addMouseListener(listener);
        addMouseMotionListener(listener);
        setDoubleBuffered(true);
        setOpaque(true); // probably accelerates rendering

        addPositionListener(new PositionListener() {
            @Override
            public void positionActive(PuertoPosition p, PuertoOffset offset, PuertoOffset offsetChange) {
                paintAllImmediately();
            }
        });
    }

    private class PositionViewerMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            requestFocusInWindow();

            if (!editable)
                return;

            mousePressedTime = e.getWhen();
            if (SwingUtilities.isLeftMouseButton(e) || SwingUtilities.isRightMouseButton(e)) {
                Point2D point = inverseTransform(painter.getPanelTrans(), e.getX(), e.getY());
                Selection prevSelected = painter.getSelected();
                if (beatChangeable && beatBounds.contains(point)) {
                    setPosition(getPosition().invertBeat());
                    painter.setSelected(Selection.NOTHING);
                } else if (painter.getManShape().contains(point)) {
                    painter.setSelected(Selection.MAN);
                } else if (painter.getLadyShape().contains(point)) {
                    painter.setSelected(Selection.LADY);
                } else {
                    painter.setSelected(Selection.NOTHING);
                }
                // repaint if selection changed
                if (!painter.getSelected().equals(prevSelected)) {
                    firstClick = true;
                    paintAllImmediately();
                } else
                    firstClick = false;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            Selection selected = painter.getSelected();
            if (!editable || selected.equals(Selection.NOTHING))
                return;

            mode = Mode.DRAGGING;
            painter.setShowArms(false);

            // calculate location
            AffineTransform trans = null;
            if (selected.equals(Selection.MAN)) {
                trans = painter.getLadyTrans();
                showManCoords = true;
                painter.setShowMan(false);
            } else if (selected.equals(Selection.LADY)) {
                trans = painter.getManTrans();
                showLadyCoords = true;
                painter.setShowLady(false);
            }
            if (trans != null) {
                Point2D point = inverseTransform(painter.getPanelTrans(), e.getX(), e.getY());
                point = inverseTransform(trans, point.getX(), point.getY());
                hoverX = (int) Math.round(point.getX() / 5.0) * 5;
                hoverY = (int) Math.round(point.getY() / 5.0) * 5;

                paintAllImmediately();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (!editable)
                return;

            PositionMutator pm = new PositionMutator(getPosition(), getOffset());
            long clickInterval = e.getWhen() - mousePressedTime;
            Selection selected = painter.getSelected();
            if (mode.equals(Mode.DRAGGING) && clickInterval > MIN_CLICK_INTERVAL) {
                if (selected.equals(Selection.LADY)) {
                    if (!(hoverX == 0 && hoverY == 0 || hoverX > 15 || hoverY > 15 || hoverX < -15 || hoverY < -15)) {
                        switch (painter.getPosition().getManDir().dir) {
                        case 0:
                            pm.moveLady(hoverX, -hoverY);
                            break;
                        case 1:
                            pm.moveLady(-hoverY, -hoverX);
                            break;
                        case 2:
                            pm.moveLady(-hoverX, hoverY);
                            break;
                        case 3:
                            pm.moveLady(hoverY, hoverX);
                            break;
                        }
                    }
                } else if (selected.equals(Selection.MAN)) {
                    if (!(hoverX == 0 && hoverY == 0 || hoverX > 15 || hoverY > 15 || hoverX < -15 || hoverY < -15)) {
                        pm.moveMan(hoverX, hoverY);
                    }
                }
            }
            mode = Mode.DEFAULT;
            painter.setShowArms(true);
            showLadyCoords = false;
            showManCoords = false;
            painter.setShowMan(true);
            painter.setShowLady(true);

            if (SwingUtilities.isLeftMouseButton(e)) {
                // a quick release causes a left rotation
                if (!firstClick && clickInterval < MIN_CLICK_INTERVAL) {
                    if (selected.equals(Selection.MAN)) {
                        pm.rotateManLeft();
                    } else if (selected.equals(Selection.LADY)) {
                        pm.rotateLadyLeft();
                    }
                }
            } else if (SwingUtilities.isRightMouseButton(e)) {
                // a quick release causes a right rotation
                if (!firstClick && clickInterval < MIN_CLICK_INTERVAL) {
                    if (selected.equals(Selection.MAN)) {
                        pm.rotateManRight();
                    } else if (selected.equals(Selection.LADY)) {
                        pm.rotateLadyRight();
                    }
                }
            }
            PuertoPosition newPosition = pm.getPosition();
            PuertoOffset newOffset = pm.getOffset();
            if (!newPosition.equals(getPosition()) || !newOffset.equals(getOffset())) {
                startCollectingUpdates();
                setPosition(newPosition);
                setOffset(newOffset);
                finishCollectingUpdates();
            } else {
                // in case the position did not change, but was in dragging mode before
                paintAllImmediately();
            }
        }
    }

    private void paintAllImmediately() {
        paintImmediately(-50000, -50000, 100000, 100000);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        painter.paintPosition(g2d, 0, 0, getWidth(), getHeight());

        if (showLadyCoords) {
            paintCoords(g2d, painter.getManTrans());
        }

        if (showManCoords) {
            paintCoords(g2d, painter.getLadyTrans());
        }
    }

    private void paintCoords(Graphics2D g, AffineTransform trans) {

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(OVERLAY_COLOR);
        g2.transform(painter.getPanelTrans());
        g2.transform(trans);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(0.2f));

        Ellipse2D ellipse = new Ellipse2D.Double();
        for (int y = -15; y <= 15; y += 5) {
            for (int x = -15; x <= 15; x += 5) {
                if (y == 0 && x == 0)
                    continue;
                ellipse.setFrame(x - 1, y - 1, 2, 2);
                if (x == hoverX && y == hoverY) {
                    g2.setColor(OVERLAY_SELECTED_COLOR);
                    g2.fill(ellipse);
                    g2.setColor(OVERLAY_COLOR);
                }
                g2.draw(ellipse);
            }
        }
    }

    private Point2D inverseTransform(AffineTransform trans, double x, double y) {
        Point2D point = new Point2D.Double();
        try {
            trans.inverseTransform(new Point2D.Double(x, y), point);
        } catch (NoninvertibleTransformException e) {
            throw new IllegalStateException(e);
        }
        return point;
    }

    /**
     * @param position the position to set
     */
    public void setPosition(PuertoPosition position) {
        if (!position.equals(painter.getPosition())) {
            painter.setPosition(position);
            updatePositionListeners();
        }
    }

    /**
     * @return the position
     */
    public PuertoPosition getPosition() {
        return painter.getPosition();
    }

    /**
     * @return the offset
     */
    public PuertoOffset getOffset() {
        return painter.getOffset();
    }

    /**
     * @return baseOffset and offset together in one offset
     */
    public PuertoOffset getCombinedOffset() {
        return painter.getCombinedOffset();
    }

    /**
     * @param offset the offset to set
     */
    public void setOffset(PuertoOffset offset) {
        if (!offset.equals(painter.getOffset())) {
            this.painter.setOffset(offset);
            // FIXME either needs offset listener, or some other solution,
            // also: how to ensure update happens only once at the end?
            updatePositionListeners();
        }
    }

    /**
     * @param baseOffset the baseOffset to set
     */
    public void setBaseOffset(PuertoOffset baseOffset) {
        if (!baseOffset.equals(painter.getBaseOffset())) {
            this.painter.setBaseOffset(baseOffset);
            updatePositionListeners();
        }
    }

    /**
     * @return the editable
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * @param editable the editable to set
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * @return the beatChangeable
     */
    public boolean isBeatChangeable() {
        return beatChangeable;
    }

    /**
     * @param beatChangeable the beatChangeable to set
     */
    public void setBeatChangeable(boolean beatChangeable) {
        this.beatChangeable = beatChangeable;
    }

    public void addPositionListener(PositionListener l) {
        positionListeners.add(l);
    }

    public void removePositionListener(PositionListener l) {
        positionListeners.remove(l);
    }

    protected void updatePositionListeners() {
        PuertoPosition p = painter.getPosition();
        PuertoOffset combinedOffset = painter.getCombinedOffset();
        PuertoOffset offset = painter.getOffset();
        if (p == null || combinedOffset == null || offset == null)
            return;
        if (suppressingUpdates > 0) {
            suppressedUpdates++;
            return;
        }
        for (PositionListener l : positionListeners) {
            try {
                l.positionActive(p, combinedOffset, offset);
            } catch (RuntimeException e) {
                // catch exceptions here to avoid unnecessary effects
                System.err.println("Exception from a PositionListener. Position: " + painter.getPosition());
                e.printStackTrace();
            }
        }
    }

    public void startCollectingUpdates() {
        suppressingUpdates++;
    }

    public void finishCollectingUpdates() {
        suppressingUpdates--;

        if (suppressingUpdates < 0)
            throw new IllegalStateException();
        if (suppressingUpdates == 0 && suppressedUpdates > 0) {
            suppressedUpdates = 0;
            updatePositionListeners();
        }
    }

    public static void main(String[] args) {
        PuertoPosition p = PuertoPosition.getInitialPosition()
                        //.withLadyDir(LadyDir.SIDE)
        .withManDir(ManDir.OPPOSITE).withFrontOffset(10)
                        //.withSideOffset(-5)
        //.withHandsJoined(HandJoint.BOTH_GRUEZI)
        .withHandsJoined(HandJoint.BOTH_JOINED).withHandsTwist(0).withRightArmAroundLady(ArmWrapped.HALF_WRAPPED);
        //.withMansRightArmAroundBody(true)
        //.withMansLeftArmAroundBody(true)
        //.withLadysLeftArmAroundBody(true)
        PuertoOffset base = PuertoOffset.getInitialOffset();
        //.withLadyLineDir(true)
        //.withAbsPosSideDir(5)
        //.withAbsPosLineDir(-5)
        PuertoOffset o = PuertoOffset.getInitialOffset();
        PositionViewer pv = new PositionViewer();
        pv.setPosition(p);
        pv.setBaseOffset(base);
        pv.setOffset(o);
        pv.setEditable(true);
        new SimplePanelFrame(pv, 600, 400);
    }
}
