/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 01.01.2010
 */
package figurabia.ui.positionviewer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.domain.PuertoPosition.ArmWrapped;
import figurabia.domain.PuertoPosition.HandHeight;
import figurabia.domain.PuertoPosition.HandJoint;
import figurabia.domain.PuertoPosition.LadyDir;
import figurabia.ui.FigurabiaBlackLookAndFeel;

public class PositionPainter {
    public enum Selection {
        NOTHING, MAN, LADY;
    }

    private PuertoPosition position;
    private PuertoOffset offset;
    private PuertoOffset baseOffset;
    private static final Path2D LADY_PATH = createLady();
    private static final Path2D MAN_PATH = createMan();
    private static final Path2D LEFT_SHOULDER = createShoulder(-1.0);
    private static final Path2D RIGHT_SHOULDER = createShoulder(1.0);
    private static final Path2D LEFT_HIP = createHip(-1.0);
    private static final Path2D RIGHT_HIP = createHip(1.0);
    private static final Path2D HANDS_N_PATH = createHandsN();
    private static final Path2D HANDS_H_PATH = createHandsH();
    private static final Path2D HANDS_E_PATH = createHandsE();
    private static final Path2D HANDS_W_PATH = createHandsW();
    private AffineTransform panelTrans;
    private Shape ladyShape;
    private Shape manShape;
    private boolean showLady = true;
    private boolean showMan = true;
    private boolean showArms = true;
    private Selection selected = Selection.NOTHING;
    private AffineTransform manTrans;
    private AffineTransform ladyTrans;
    private static final double ARM_LINE_MARGIN = 0.5;
    private static final float ARM_WIDTH = 0.3f;
    private static final Color AXIS_COLOR;
    private static final Color PERSON_COLOR;
    private static final Color SELECTED_COLOR;
    private static final Color ARM_COLOR;
    private static final Color BACKGROUND_COLOR;
    static {
        if (UIManager.getLookAndFeel() instanceof FigurabiaBlackLookAndFeel) {
            AXIS_COLOR = Color.GRAY;
            PERSON_COLOR = Color.DARK_GRAY;
            SELECTED_COLOR = new Color(255, 204, 0);
            ARM_COLOR = Color.LIGHT_GRAY;
            BACKGROUND_COLOR = Color.BLACK;
        } else {
            AXIS_COLOR = Color.LIGHT_GRAY;
            PERSON_COLOR = Color.GRAY;
            SELECTED_COLOR = new Color(255, 204, 0);
            ARM_COLOR = Color.BLACK;
            BACKGROUND_COLOR = Color.WHITE;
        }
    }

    public PuertoPosition getPosition() {
        return position;
    }

    public void setPosition(PuertoPosition position) {
        this.position = position;
    }

    public PuertoOffset getOffset() {
        return offset;
    }

    public void setOffset(PuertoOffset offset) {
        this.offset = offset;
    }

    public PuertoOffset getBaseOffset() {
        return baseOffset;
    }

    public void setBaseOffset(PuertoOffset baseOffset) {
        this.baseOffset = baseOffset;
    }

    public PuertoOffset getCombinedOffset() {
        if (baseOffset == null || offset == null)
            return null;
        return baseOffset.addOffset(offset);
    }

    public AffineTransform getPanelTrans() {
        return panelTrans;
    }

    public void setPanelTrans(AffineTransform panelTrans) {
        this.panelTrans = panelTrans;
    }

    public Shape getLadyShape() {
        return ladyShape;
    }

    public void setLadyShape(Shape ladyShape) {
        this.ladyShape = ladyShape;
    }

    public Shape getManShape() {
        return manShape;
    }

    public void setManShape(Shape manShape) {
        this.manShape = manShape;
    }

    public boolean isShowLady() {
        return showLady;
    }

    public void setShowLady(boolean showLady) {
        this.showLady = showLady;
    }

    public boolean isShowMan() {
        return showMan;
    }

    public void setShowMan(boolean showMan) {
        this.showMan = showMan;
    }

    public boolean isShowArms() {
        return showArms;
    }

    public void setShowArms(boolean showArms) {
        this.showArms = showArms;
    }

    public Selection getSelected() {
        return selected;
    }

    public void setSelected(Selection selected) {
        this.selected = selected;
    }

    public AffineTransform getManTrans() {
        return manTrans;
    }

    public void setManTrans(AffineTransform manTrans) {
        this.manTrans = manTrans;
    }

    public AffineTransform getLadyTrans() {
        return ladyTrans;
    }

    public void setLadyTrans(AffineTransform ladyTrans) {
        this.ladyTrans = ladyTrans;
    }

    public void paintPosition(Graphics2D g, int x, int y, int width, int height) {
        double internalWidth = 40;

        g = (Graphics2D) g.create();
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(x, y, width, height);

        // transform to a -20 to 20 area in both dimensions (square), origin is in the center
        double sideLength = Math.min(width, height);
        panelTrans = new AffineTransform();
        panelTrans.translate(x + width / 2.0, y + height / 2.0);
        panelTrans.scale(sideLength / internalWidth, sideLength / internalWidth);
        g.transform(panelTrans);

        paintPositionContent(g, internalWidth);
    }

    public void paintCompactPosition(Graphics2D g, int x, int y, int width, int height) {
        double internalWidth = 20;

        g = (Graphics2D) g.create();
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(x, y, width, height);

        // transform to a -7.5 to 7.5 area in both dimensions (square), origin is in the center
        double sideLength = Math.min(width, height);
        panelTrans = new AffineTransform();
        panelTrans.translate(x + width / 2.0, y + height / 2.0);
        panelTrans.scale(sideLength / internalWidth, sideLength / internalWidth);
        g.transform(panelTrans);

        // adjust offset to make the position fit into the square
        if (position.getLadyDir() == LadyDir.ON_LINE) {
            int correctX = (position.getFrontOffset() - 10) / 2;
            int correctY = -position.getSideOffset() / 2;
            offset = new PuertoOffset(false, correctX, correctY);
        } else {
            int correctX = (position.getSideOffset() - 10) / 2;
            int correctY = position.getFrontOffset() / 2;
            offset = new PuertoOffset(false, correctX, correctY);
        }

        paintPositionContent(g, internalWidth);
    }

    private void paintPositionContent(Graphics2D g, double width) {

        if (baseOffset == null || offset == null || position == null)
            return;

        // create offset combined from previous offset and delta
        PuertoOffset combined = baseOffset.addOffset(offset);

        // calculate dancer's body's positions
        double ladyX = 5 + combined.getAbsPosLineDir();
        double ladyY = combined.getAbsPosSideDir();
        int ladyDir = (combined.isLadyLineDir() ? 2 : 0) + (position.getLadyDir().equals(LadyDir.ON_LINE) ? 0 : 1);
        ladyTrans = createTransform(ladyX, ladyY, ladyDir * Math.PI / 2.0);
        double manX = ladyX + calcSign(ladyDir) * position.getFrontOffset() + calcSign(ladyDir + 3)
                * position.getSideOffset(); // sign depends on direction of the lady
        double manY = ladyY + calcSign(ladyDir + 2) * position.getSideOffset() + calcSign(ladyDir + 3)
                * position.getFrontOffset(); // could be different based on lady's direction (front/side offset)
        int manDir = ladyDir + position.getManDir().dir;
        manTrans = createTransform(manX, manY, manDir * Math.PI / 2.0);

        g.setColor(AXIS_COLOR);

        g.setStroke(new BasicStroke(0.1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f,
                new float[] { 1.0f, 1.0f }, 0));
        g.draw(new Line2D.Double(-width / 2.0, 0, width / 2.0, 0));

        g.setStroke(new BasicStroke(0.2f));

        if (showLady) {
            if (selected.equals(Selection.LADY))
                g.setColor(SELECTED_COLOR);
            else
                g.setColor(PERSON_COLOR);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            ladyShape = paintShape(g, LADY_PATH, ladyTrans);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
            //g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        }
        if (showMan) {
            if (selected.equals(Selection.MAN))
                g.setColor(SELECTED_COLOR);
            else
                g.setColor(PERSON_COLOR);
            manShape = paintShape(g, MAN_PATH, manTrans);
        }

        if (showArms) {
            // commented out, because it makes the arms look too blurry in some situations
            //g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawArms(g, ladyTrans, manTrans);
        }

        // draw beat
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 5f));
        // TODO string should not be drawn in an integer location (not exact enough)
        g.drawString(String.valueOf(position.getBeat()), (int) (width / 2.0 - 3.0), (int) (-(width / 2.0 - 4.0)));
    }

    private void drawArms(Graphics2D g2d, AffineTransform ladyTrans, AffineTransform manTrans) {
        // calculate arms
        List<Point2D> leftArmPoints = new ArrayList<Point2D>();
        List<Point2D> rightArmPoints = new ArrayList<Point2D>();
        List<Integer> leftArmLineZ = new ArrayList<Integer>();
        List<Integer> rightArmLineZ = new ArrayList<Integer>();

        // determine if lady's part has to be switched (because BOTH_GREET
        HandJoint joined = position.getHandsJoined();
        boolean doSwitch = joined.equals(HandJoint.BOTH_GREET) || joined.equals(HandJoint.LEFT_GREET)
                || joined.equals(HandJoint.RIGHT_GREET);

        // start at man's arms
        leftArmPoints.add(transform(manTrans, 0.0, -4.5));
        rightArmPoints.add(transform(manTrans, 0.0, 4.5));

        Point2D manHandsBoxPoint;
        if (position.getLeftArmAroundMan() == ArmWrapped.HALF_WRAPPED
                && position.getRightArmAroundMan() == ArmWrapped.HALF_WRAPPED) {
            manHandsBoxPoint = transform(manTrans, -0.5, 0.0);
        } else if (position.getLeftArmAroundMan() == ArmWrapped.HALF_WRAPPED) {
            manHandsBoxPoint = transform(manTrans, -0.5, 3.75);
        } else if (position.getRightArmAroundMan() == ArmWrapped.HALF_WRAPPED) {
            manHandsBoxPoint = transform(manTrans, -0.5, -3.75); // 1.5 instead of 1.0
        } else {
            manHandsBoxPoint = transform(manTrans, -0.5, 0.0); // 3.5 instead of 1.0
        }
        Point2D ladyHandsBoxPoint;
        if (position.getLeftArmAroundLady() == ArmWrapped.HALF_WRAPPED
                && position.getRightArmAroundLady() == ArmWrapped.HALF_WRAPPED) {
            ladyHandsBoxPoint = transform(ladyTrans, 0.5, 0.0);
        } else if (position.getLeftArmAroundLady() == ArmWrapped.HALF_WRAPPED) {
            ladyHandsBoxPoint = transform(ladyTrans, 0.5, doSwitch ? -3.75 : 3.75);
        } else if (position.getRightArmAroundLady() == ArmWrapped.HALF_WRAPPED) {
            ladyHandsBoxPoint = transform(ladyTrans, 0.5, doSwitch ? 3.75 : -3.75); // -1.5 instead of -1.0
        } else {
            ladyHandsBoxPoint = transform(ladyTrans, 0.5, 0.0); // -3.5 instead of -1.0
        }

        Point2D handsBoxCenter = mid(manHandsBoxPoint, ladyHandsBoxPoint);
        AffineTransform boxTrans = createTransform(handsBoxCenter, ladyHandsBoxPoint);
        Point2D boxManLeft = transform(boxTrans, -3.0, -2.0);
        Point2D boxManRight = transform(boxTrans, -3.0, 2.0);
        Point2D boxVec = vector(boxManLeft, boxManRight);
        Point2D manVec = vector(leftArmPoints.get(0), rightArmPoints.get(0));

        boolean ladyBehindMan = !isWithin90(boxVec, manVec, true);
        boolean manTwisted = ladyBehindMan;
        if (position.getLeftArmAroundMan() == ArmWrapped.HALF_WRAPPED
                && position.getRightArmAroundMan() == ArmWrapped.HALF_WRAPPED)
            manTwisted = !manTwisted;

        Point2D ladyLeftArmPoint = transform(ladyTrans, 0.0, -4.5);
        Point2D ladyRightArmPoint = transform(ladyTrans, 0.0, 4.5);
        Point2D ladyVec = vector(ladyLeftArmPoint, ladyRightArmPoint);

        boolean manBehindLady = !isWithin90(boxVec, ladyVec, true);
        boolean ladyTwisted = manBehindLady;
        if (position.getLeftArmAroundLady() == ArmWrapped.HALF_WRAPPED
                && position.getRightArmAroundLady() == ArmWrapped.HALF_WRAPPED)
            ladyTwisted = !ladyTwisted;

        // around man's body if specified
        if (position.getLeftArmAroundMan() == ArmWrapped.HALF_WRAPPED) {
            double sign = ladyBehindMan ? -1 : 1;
            //leftArmPoints.add(transform(manTrans, sign * -2.3, -3.4));
            //leftArmPoints.add(transform(manTrans, sign * -2.3, 4.2));
            //leftArmPoints.add(transform(manTrans, 0.0, 5.5));
            leftArmPoints.add(transform(manTrans, sign * -1.5, 0.0));
            leftArmPoints.add(transform(manTrans, 0.0, 3.0));
            leftArmLineZ.add(2);
            leftArmLineZ.add(2);
            leftArmLineZ.add(2);
        } else if (position.getLeftArmAroundMan() == ArmWrapped.FULLY_WRAPPED) {
            double sign = ladyBehindMan ? -1 : 1;
            leftArmPoints.add(transform(manTrans, sign * 1.5, 0.0));
            leftArmPoints.add(transform(manTrans, 0.0, 3.0));
            leftArmPoints.add(transform(manTrans, sign * -1.0, 0.0));
            leftArmPoints.add(transform(manTrans, 0.0, -3.0));
            leftArmLineZ.add(2);
            leftArmLineZ.add(2);
            leftArmLineZ.add(2);
            leftArmLineZ.add(2);
        }
        if (position.getRightArmAroundMan() == ArmWrapped.HALF_WRAPPED) {
            double sign = ladyBehindMan ? -1 : 1;
            //rightArmPoints.add(transform(manTrans, sign * -1.7, 3.8));
            //rightArmPoints.add(transform(manTrans, sign * -1.7, -4.3));
            //rightArmPoints.add(transform(manTrans, 0.0, -5.5));
            rightArmPoints.add(transform(manTrans, sign * -1.5, 0.0));
            rightArmPoints.add(transform(manTrans, 0.0, -3.0));
            rightArmLineZ.add(2);
            rightArmLineZ.add(2);
            rightArmLineZ.add(2);
        } else if (position.getRightArmAroundMan() == ArmWrapped.FULLY_WRAPPED) {
            double sign = ladyBehindMan ? -1 : 1;
            rightArmPoints.add(transform(manTrans, sign * 1.5, 0.0));
            rightArmPoints.add(transform(manTrans, 0.0, -3.0));
            rightArmPoints.add(transform(manTrans, sign * -1.0, 0.0));
            rightArmPoints.add(transform(manTrans, 0.0, 3.0));
            rightArmLineZ.add(2);
            rightArmLineZ.add(2);
            rightArmLineZ.add(2);
            rightArmLineZ.add(2);
        }

        // select appropriate box
        int boxType = -1000; // -1000 means no box
        switch (position.getHandsJoined()) {
        case BOTH_JOINED:
            boxType = 2 * position.getHandsTwist()
                    + ((manTwisted || ladyTwisted) && !(manTwisted && ladyTwisted) ? -1 : 0);
            break;
        case BOTH_GREET:
            boxType = 2 * position.getHandsTwist() - 1
                    + ((manTwisted || ladyTwisted) && !(manTwisted && ladyTwisted) ? 1 : 0);
            break;
        }

        List<Point2D> boxLeftPoints;
        List<Integer> boxLeftLineZ;
        List<Point2D> boxRightPoints;
        List<Integer> boxRightLineZ;
        if (manTwisted) {
            boxLeftPoints = rightArmPoints;
            boxLeftLineZ = rightArmLineZ;
            boxRightPoints = leftArmPoints;
            boxRightLineZ = leftArmLineZ;
        } else {
            boxLeftPoints = leftArmPoints;
            boxLeftLineZ = leftArmLineZ;
            boxRightPoints = rightArmPoints;
            boxRightLineZ = rightArmLineZ;
        }

        // add box
        switch (boxType) {
        case -3:
            boxLeftPoints.add(transform(boxTrans, -3.0, -2.0));
            boxLeftLineZ.add(0);
            boxRightPoints.add(transform(boxTrans, -3.0, 2.0));
            boxRightLineZ.add(0);

            boxLeftPoints.add(transform(boxTrans, -1.0, 0.0));
            boxLeftLineZ.add(-1);
            boxLeftPoints.add(transform(boxTrans, 1.0, -3.0));
            boxLeftLineZ.add(1);
            boxRightPoints.add(transform(boxTrans, -1.0, -3.0));
            boxRightLineZ.add(1);
            boxRightPoints.add(transform(boxTrans, 1.0, 0.0));
            boxRightLineZ.add(-1);

            boxLeftPoints.add(transform(boxTrans, 3.0, 2.0));
            boxLeftLineZ.add(-1);
            boxRightPoints.add(transform(boxTrans, 3.0, -2.0));
            boxRightLineZ.add(1);
            break;
        case -2:
            boxLeftPoints.add(transform(boxTrans, -3.0, -2.0));
            boxLeftLineZ.add(0);
            boxRightPoints.add(transform(boxTrans, -3.0, 2.0));
            boxRightLineZ.add(0);
            boxRightPoints.add(transform(boxTrans, 0.0, -3.0));
            boxRightLineZ.add(1);
            boxLeftPoints.add(transform(boxTrans, 3.0, -2.0));
            boxLeftLineZ.add(0);
            boxRightPoints.add(transform(boxTrans, 3.0, 2.0));
            boxRightLineZ.add(-1);
            break;
        case -1:
            boxLeftPoints.add(transform(boxTrans, -3.0, -2.0));
            boxLeftLineZ.add(0);
            boxRightPoints.add(transform(boxTrans, -3.0, 2.0));
            boxRightLineZ.add(0);
            boxLeftPoints.add(transform(boxTrans, 3.0, 2.0));
            boxLeftLineZ.add(-1);
            boxRightPoints.add(transform(boxTrans, 3.0, -2.0));
            boxRightLineZ.add(1);
            break;
        case 0:
            // nothing to do (direct connection)
            break;
        case 1:
            boxLeftPoints.add(transform(boxTrans, -3.0, -2.0));
            boxLeftLineZ.add(0);
            boxRightPoints.add(transform(boxTrans, -3.0, 2.0));
            boxRightLineZ.add(0);
            boxLeftPoints.add(transform(boxTrans, 3.0, 2.0));
            boxLeftLineZ.add(1);
            boxRightPoints.add(transform(boxTrans, 3.0, -2.0));
            boxRightLineZ.add(-1);
            break;
        case 2:
            boxLeftPoints.add(transform(boxTrans, -3.0, -2.0));
            boxLeftLineZ.add(0);
            boxRightPoints.add(transform(boxTrans, -3.0, 2.0));
            boxRightLineZ.add(0);
            boxLeftPoints.add(transform(boxTrans, 0.0, 3.0));
            boxLeftLineZ.add(1);
            boxLeftPoints.add(transform(boxTrans, 3.0, -2.0));
            boxLeftLineZ.add(-1);
            boxRightPoints.add(transform(boxTrans, 3.0, 2.0));
            boxRightLineZ.add(0);
            break;
        case 3:
            boxLeftPoints.add(transform(boxTrans, -3.0, -2.0));
            boxLeftLineZ.add(0);
            boxRightPoints.add(transform(boxTrans, -3.0, 2.0));
            boxRightLineZ.add(0);

            boxRightPoints.add(transform(boxTrans, -1.0, 0.0));
            boxRightLineZ.add(-1);
            boxRightPoints.add(transform(boxTrans, 1.0, 3.0));
            boxRightLineZ.add(1);
            boxLeftPoints.add(transform(boxTrans, -1.0, 3.0));
            boxLeftLineZ.add(1);
            boxLeftPoints.add(transform(boxTrans, 1.0, 0.0));
            boxLeftLineZ.add(-1);

            boxLeftPoints.add(transform(boxTrans, 3.0, 2.0));
            boxLeftLineZ.add(1);
            boxRightPoints.add(transform(boxTrans, 3.0, -2.0));
            boxRightLineZ.add(-1);
            break;
        }

        // switch lists if it is a greet position (will be switched back at the end)
        boolean leftAroundLadysBody = position.getLeftArmAroundLady() == ArmWrapped.HALF_WRAPPED;
        boolean leftFullyAroundLady = position.getLeftArmAroundLady() == ArmWrapped.FULLY_WRAPPED;
        boolean rightAroundLadysBody = position.getRightArmAroundLady() == ArmWrapped.HALF_WRAPPED;
        boolean rightFullyAroundLady = position.getRightArmAroundLady() == ArmWrapped.FULLY_WRAPPED;
        if (doSwitch) {
            List<Point2D> tmpPoints = leftArmPoints;
            List<Integer> tmpLineZ = leftArmLineZ;
            boolean tmpAroundBody = leftAroundLadysBody;
            boolean tmpFullyAround = leftFullyAroundLady;
            leftArmPoints = rightArmPoints;
            leftArmLineZ = rightArmLineZ;
            leftAroundLadysBody = rightAroundLadysBody;
            leftFullyAroundLady = rightFullyAroundLady;
            rightArmPoints = tmpPoints;
            rightArmLineZ = tmpLineZ;
            rightAroundLadysBody = tmpAroundBody;
            rightFullyAroundLady = tmpFullyAround;
        }

        // around lady's body if specified
        if (leftAroundLadysBody) {
            double sign = manBehindLady ? -1 : 1;
            //leftArmPoints.add(transform(ladyTrans, 0.0, 5.5));
            //leftArmPoints.add(transform(ladyTrans, sign * 2.3, 4.2));
            //leftArmPoints.add(transform(ladyTrans, sign * 2.3, -3.4));
            leftArmPoints.add(transform(ladyTrans, 0.0, 3.0));
            leftArmPoints.add(transform(ladyTrans, sign * 1.0, 0.0));
            leftArmLineZ.add(2);
            leftArmLineZ.add(2);
            leftArmLineZ.add(2);
        } else if (leftFullyAroundLady) {
            double sign = manBehindLady ? -1 : 1;
            leftArmPoints.add(transform(ladyTrans, 0.0, -3.0));
            leftArmPoints.add(transform(ladyTrans, sign * 1.0, 0.0));
            leftArmPoints.add(transform(ladyTrans, 0.0, 3.0));
            leftArmPoints.add(transform(ladyTrans, sign * -1.5, 0.0));
            leftArmLineZ.add(2);
            leftArmLineZ.add(2);
            leftArmLineZ.add(2);
            leftArmLineZ.add(2);
        }
        if (rightAroundLadysBody) {
            double sign = manBehindLady ? -1 : 1;
            //rightArmPoints.add(transform(ladyTrans, 0.0, -5.5));
            //rightArmPoints.add(transform(ladyTrans, sign * 1.7, -4.3));
            //rightArmPoints.add(transform(ladyTrans, sign * 1.7, 3.8));
            rightArmPoints.add(transform(ladyTrans, 0.0, -3.0));
            rightArmPoints.add(transform(ladyTrans, sign * 1.5, 0.0));
            rightArmLineZ.add(2);
            rightArmLineZ.add(2);
            rightArmLineZ.add(2);
        } else if (rightFullyAroundLady) {
            double sign = manBehindLady ? -1 : 1;
            rightArmPoints.add(transform(ladyTrans, 0.0, 3.0));
            rightArmPoints.add(transform(ladyTrans, sign * 1.0, 0.0));
            rightArmPoints.add(transform(ladyTrans, 0.0, -3.0));
            rightArmPoints.add(transform(ladyTrans, sign * -1.5, 0.0));
            rightArmLineZ.add(2);
            rightArmLineZ.add(2);
            rightArmLineZ.add(2);
            rightArmLineZ.add(2);
        }

        // stop at lady's arms
        leftArmPoints.add(ladyLeftArmPoint);
        rightArmPoints.add(ladyRightArmPoint);
        leftArmLineZ.add(0);
        rightArmLineZ.add(0);

        // switch lists back if switched above
        if (doSwitch) {
            List<Point2D> tmpPoints = leftArmPoints;
            List<Integer> tmpLineZ = leftArmLineZ;
            leftArmPoints = rightArmPoints;
            leftArmLineZ = rightArmLineZ;
            rightArmPoints = tmpPoints;
            rightArmLineZ = tmpLineZ;
        }

        // draw lines
        g2d.setStroke(new BasicStroke(ARM_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(ARM_COLOR);
        switch (position.getHandsJoined()) {
        case FREE:
            // nothing to draw
            break;
        case LEFT_JOINED:
            // simple case
            drawSingleHandsLine(g2d, leftArmPoints);
            break;
        case RIGHT_JOINED:
            drawSingleHandsLine(g2d, rightArmPoints);
            break;
        case LEFT_GREET:
            drawSingleHandsLine(g2d, leftArmPoints);
            break;
        case RIGHT_GREET:
            drawSingleHandsLine(g2d, rightArmPoints);
            break;
        case BOTH_JOINED:
            // complex case 1
            drawHandsLines(g2d, leftArmPoints, leftArmLineZ, rightArmPoints, rightArmLineZ);
            drawHandsLines(g2d, rightArmPoints, rightArmLineZ, leftArmPoints, leftArmLineZ);
            break;
        case BOTH_GREET:
            // complex case 2
            drawHandsLines(g2d, leftArmPoints, leftArmLineZ, rightArmPoints, rightArmLineZ);
            drawHandsLines(g2d, rightArmPoints, rightArmLineZ, leftArmPoints, leftArmLineZ);
            break;
        }

        // draw hand height
        drawHandHeight(g2d, manTrans, doSwitch ? +1 : -1, -1, position.getLeftHandHeight());
        drawHandHeight(g2d, manTrans, doSwitch ? -1 : +1, +1, position.getRightHandHeight());
    }

    private void drawHandHeight(Graphics2D g, AffineTransform boxTrans, int ladySign, int manSign, HandHeight handHeight) {
        switch (handHeight) {
        case NORMAL:
            // nothing to do
            break;
        case ON_SHOULDER:
            drawShoulder(g, ladySign);
            break;
        case ON_HIP:
            drawHip(g, ladySign);
            break;
        case NECK_LOCKED:
            drawHandsCharacter(g, boxTrans, manSign, HANDS_N_PATH);
            break;
        case HIP_LOCKED:
            drawHandsCharacter(g, boxTrans, manSign, HANDS_H_PATH);
            break;
        case ELBOW_LOCKED:
            drawHandsCharacter(g, boxTrans, manSign, HANDS_E_PATH);
            break;
        case HAND_LOCKED:
            drawHandsCharacter(g, boxTrans, manSign, HANDS_W_PATH);
            break;
        }
    }

    private void drawShoulder(Graphics2D g, int sign) {
        g = (Graphics2D) g.create();
        g.transform(ladyTrans);
        g.setStroke(new BasicStroke(ARM_WIDTH * 2.0f));
        g.draw(sign == -1 ? LEFT_SHOULDER : RIGHT_SHOULDER);
    }

    private void drawHip(Graphics2D g, int sign) {
        g = (Graphics2D) g.create();
        g.transform(ladyTrans);
        g.setStroke(new BasicStroke(ARM_WIDTH * 2.0f));
        g.draw(sign == -1 ? LEFT_HIP : RIGHT_HIP);
    }

    private void drawHandsCharacter(Graphics2D g, AffineTransform boxTrans, int sign, Path2D characterPath) {
        g = (Graphics2D) g.create();
        Point2D pointOnBox = new Point2D.Double(0.0, sign * 8.0);
        Point2D transformed = new Point2D.Double();
        boxTrans.transform(pointOnBox, transformed);
        g.translate(transformed.getX(), transformed.getY());
        g.fill(characterPath);
    }

    private void drawHandsLines(Graphics2D g, List<Point2D> armPoints, List<Integer> armLineZ,
            List<Point2D> crossingArmPoints, List<Integer> crossingArmLineZ) {

        for (int i = 1; i < armPoints.size(); i++) {

            Line2D lineToDraw = new Line2D.Double(armPoints.get(i - 1), armPoints.get(i));
            int z = armLineZ.get(i - 1);

            // find all intersections and create clip
            Path2D clippingPath = new Path2D.Double(Path2D.WIND_EVEN_ODD);
            clippingPath.append(createRectangleForLine(lineToDraw.getX1(), lineToDraw.getY1(), lineToDraw.getX2(),
                    lineToDraw.getY2(), ARM_LINE_MARGIN), false);
            // intersections of crossing arm
            for (int j = 1; j < crossingArmPoints.size(); j++) {

                Point2D from = crossingArmPoints.get(j - 1);
                Point2D to = crossingArmPoints.get(j);
                if (z < crossingArmLineZ.get(j - 1)
                        && lineToDraw.intersectsLine(from.getX(), from.getY(), to.getX(), to.getY())) {
                    clippingPath.append(createRectangleForLine(from.getX(), from.getY(), to.getX(), to.getY(),
                            ARM_LINE_MARGIN), false);
                }
            }
            // intersections of arm itself
            for (int j = 1; j < armPoints.size(); j++) {
                if (Math.abs(i - j) <= 1)
                    continue;
                Point2D from = armPoints.get(j - 1);
                Point2D to = armPoints.get(j);
                if (z < armLineZ.get(j - 1)
                        && lineToDraw.intersectsLine(from.getX(), from.getY(), to.getX(), to.getY())) {
                    clippingPath.append(createRectangleForLine(from.getX(), from.getY(), to.getX(), to.getY(),
                            ARM_LINE_MARGIN), false);
                }
            }

            // draw line
            Shape oldClip = g.getClip();
            g.setClip(clippingPath);
            g.draw(lineToDraw);
            g.setClip(oldClip);
        }
    }

    private void drawSingleHandsLine(Graphics2D g, List<Point2D> points) {
        Line2D line = new Line2D.Double(new Point2D.Double(), points.get(0));
        for (int i = 1; i < points.size(); i++) {
            line.setLine(line.getP2(), points.get(i));
            g.draw(line);
        }
    }

    private static Path2D createLady() {
        double sqrt2 = Math.sqrt(2.0);
        Path2D path = new Path2D.Double();
        path.moveTo(-1.5, -4.5);
        path.curveTo(-1.5 + 0.25 * sqrt2, -4.5 - 0.25 * sqrt2, -1.0, -5.0, -0.5, -5.0);
        path.curveTo(0.5, -5.0, 1.0, -4.5, 1.0, -3.5);
        path.curveTo(1.0, -2.75, 0.5, -2.25, 0.5, -1.5);
        path.curveTo(0.5, -0.5, 1.5, 0.0, 1.5, 0.0);

        path.curveTo(1.5, 0.0, 0.5, 0.5, 0.5, 1.5);
        path.curveTo(0.5, 2.25, 1.0, 2.75, 1.0, 3.5);
        path.curveTo(1.0, 4.5, 0.5, 5.0, -0.5, 5.0);
        path.curveTo(-1.0, 5.0, -1.5 + 0.25 * sqrt2, 4.5 + 0.25 * sqrt2, -1.5, 4.5);

        path.curveTo(-1.5, 4.5, 0.0, 4.75, 0.0, 4.0);
        path.curveTo(0.0, 3.0, -1.0, 2.5, -1.0, 1.5);
        path.curveTo(-1.0, 0.5, 0.5, 0.0, 0.5, 0.0);

        path.curveTo(0.5, 0.0, -1.0, -0.5, -1.0, -1.5);
        path.curveTo(-1.0, -2.5, 0.0, -3.0, 0.0, -4.0);
        path.curveTo(0.0, -4.75, -1.5, -4.5, -1.5, -4.5);

        path.closePath();
        return path;
    }

    private static Path2D createMan() {
        Path2D path = new Path2D.Double();
        path.moveTo(-1.5, -5.0);
        path.lineTo(1.5, -5.0);
        path.lineTo(1.5, -4.0);
        path.lineTo(0, -4.0);
        path.lineTo(0, 4.0);
        path.lineTo(1.5, 4.0);
        path.lineTo(1.5, 5.0);
        path.lineTo(-1.5, 5.0);
        path.closePath();
        return path;
    }

    private static Path2D createShoulder(double sign) {
        Path2D path = new Path2D.Double();
        path.moveTo(-0.5, sign * 4.5);
        path.lineTo(1.0, sign * 4.5);
        path.lineTo(1.0, sign * 3.0);
        return path;
    }

    private static Path2D createHip(double sign) {
        Path2D path = new Path2D.Double();
        path.moveTo(-1.0, sign * 3.5);
        path.quadTo(-1.0, sign * 4.5, 0.0, sign * 4.5);
        path.quadTo(1.0, sign * 4.5, 1.0, sign * 3.5);
        return path;
    }

    private static Path2D createHandsN() {
        Path2D path = new Path2D.Double();
        path.moveTo(-1.0, -1.0);
        path.lineTo(-0.4, -1.0);
        path.lineTo(0.4, 0.4);
        path.lineTo(0.4, -1.0);
        path.lineTo(1.0, -1.0);
        path.lineTo(1.0, 1.0);
        path.lineTo(0.4, 1.0);
        path.lineTo(-0.4, -0.4);
        path.lineTo(-0.4, 1.0);
        path.lineTo(-1.0, 1.0);
        path.closePath();
        return path;
    }

    private static Path2D createHandsH() {
        Path2D path = new Path2D.Double();
        path.moveTo(-1.0, -1.0);
        path.lineTo(-0.4, -1.0);
        path.lineTo(-0.4, -0.3);
        path.lineTo(0.4, -0.3);
        path.lineTo(0.4, -1.0);
        path.lineTo(1.0, -1.0);
        path.lineTo(1.0, 1.0);
        path.lineTo(0.4, 1.0);
        path.lineTo(0.4, 0.3);
        path.lineTo(-0.4, 0.3);
        path.lineTo(-0.4, 1.0);
        path.lineTo(-1.0, 1.0);
        path.closePath();
        return path;
    }

    private static Path2D createHandsE() {
        Path2D path = new Path2D.Double();
        path.moveTo(-1.0, -1.0);
        path.lineTo(1.0, -1.0);
        path.lineTo(1.0, -0.6);
        path.lineTo(-0.4, -0.6);
        path.lineTo(-0.4, -0.2);
        path.lineTo(1.0, -0.2);
        path.lineTo(1.0, 0.2);
        path.lineTo(-0.4, 0.2);
        path.lineTo(-0.4, 0.6);
        path.lineTo(1.0, 0.6);
        path.lineTo(1.0, 1.0);
        path.lineTo(-1.0, 1.0);
        path.closePath();
        return path;
    }

    private static Path2D createHandsW() {
        Path2D path = new Path2D.Double();
        path.moveTo(-1.0, -1.0);
        path.lineTo(-0.6, -1.0);
        path.lineTo(-0.4, 0.2);
        path.lineTo(0.0, -0.2);
        path.lineTo(0.4, 0.2);
        path.lineTo(0.6, -1.0);
        path.lineTo(1.0, -1.0);
        path.lineTo(0.6, 1.0);
        path.lineTo(0.2, 1.0);
        path.lineTo(0.0, 0.2);
        path.lineTo(-0.2, 1.0);
        path.lineTo(-0.6, 1.0);
        path.closePath();
        return path;
    }

    private int calcSign(int ladyDir) {
        ladyDir = ladyDir % 4;
        switch (ladyDir) {
        case 0:
            return -1;
        case 1:
            return 0;
        case 2:
            return 1;
        case 3:
            return 0;
        }
        throw new IllegalStateException("illegal ladyDir value: " + ladyDir);
    }

    private Shape createRectangleForLine(double x1, double y1, double x2, double y2, double margin) {
        double v1x = x2 - x1;
        double v1y = y2 - y1;
        double vd = Math.sqrt(v1x * v1x + v1y * v1y);
        v1x /= vd;
        v1y /= vd;
        double v2x = v1y;
        double v2y = -v1x;

        Path2D path = new Path2D.Double();
        path.moveTo(x1 - margin * v1x + margin * v2x, y1 - margin * v1y + margin * v2y);
        path.lineTo(x1 - margin * v1x - margin * v2x, y1 - margin * v1y - margin * v2y);
        path.lineTo(x2 + margin * v1x - margin * v2x, y2 + margin * v1y - margin * v2y);
        path.lineTo(x2 + margin * v1x + margin * v2x, y2 + margin * v1y + margin * v2y);
        path.closePath();

        return path;
    }

    private AffineTransform createTransform(double x, double y, double angle) {
        AffineTransform trans = new AffineTransform();
        trans.translate(x, y);
        trans.rotate(angle);
        return trans;
    }

    /**
     * Calculates a transformation that transforms the origin (0,0) to the given origin point and the x axis in
     * direction of the given direction
     */
    private AffineTransform createTransform(Point2D origin, Point2D direction) {
        return createTransform(origin.getX(), origin.getY(), Math.atan2(direction.getY() - origin.getY(),
                direction.getX() - origin.getX()));
    }

    private boolean isWithin90(Point2D vec1, Point2D vec2, boolean equalCase) {
        // dot product
        double dotProduct = vec1.getX() * vec2.getX() + vec1.getY() * vec2.getY();
        if (Math.abs(dotProduct) < 1e-9)
            return equalCase;
        return dotProduct > 0;
    }

    private Point2D mid(Point2D a, Point2D b) {
        return new Point2D.Double((a.getX() + b.getX()) / 2.0, (a.getY() + b.getY()) / 2.0);
    }

    private Shape paintShape(Graphics2D g, Path2D path, AffineTransform trans) {
        path = (Path2D) path.clone();
        path.transform(trans);
        g.fill(path);
        return path;
    }

    private Point2D transform(AffineTransform trans, double x, double y) {
        Point2D point = new Point2D.Double();
        trans.transform(new Point2D.Double(x, y), point);
        return point;
    }

    private Point2D vector(Point2D origin, Point2D point) {
        return new Point2D.Double(point.getX() - origin.getX(), point.getY() - origin.getY());
    }
}
