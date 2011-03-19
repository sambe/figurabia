/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 13.05.2010
 */
package figurabia.domain.util;

import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.domain.PuertoPosition.LadyDir;
import figurabia.domain.PuertoPosition.ManDir;

public class PositionMutator {

    private PuertoPosition position;
    private PuertoOffset offset;

    public PositionMutator(PuertoPosition position, PuertoOffset offset) {
        super();
        this.position = position;
        this.offset = offset;
    }

    /**
     * @return the position
     */
    public PuertoPosition getPosition() {
        return position;
    }

    /**
     * @return the offset
     */
    public PuertoOffset getOffset() {
        return offset;
    }

    /**
     * Moves the lady (leaves the man where he is).
     * 
     * @param front the new value of front (seen from the man)
     * @param side the new value of side (seen from the man)
     */
    public void moveLady(int front, int side) {
        int absLine = offset.getAbsPosLineDir();
        int absSide = offset.getAbsPosSideDir();
        int dir = (offset.isLadyLineDir() ? 2 : 0) + (position.getLadyDir().equals(LadyDir.ON_LINE) ? 0 : 1);
        switch (dir % 4) {
        case 0:
            absLine += front - position.getFrontOffset();
            absSide -= side - position.getSideOffset();
            break;
        case 1:
            absLine += side - position.getSideOffset();
            absSide += front - position.getFrontOffset();
            break;
        case 2:
            absLine -= front - position.getFrontOffset();
            absSide += side - position.getSideOffset();
            break;
        case 3:
            absLine -= side - position.getSideOffset();
            absSide -= front - position.getFrontOffset();
            break;
        }
        offset = offset.withAbsPosLineDir(absLine).withAbsPosSideDir(absSide);

        position = position.withFrontOffset(front).withSideOffset(side);
    }

    public void moveMan(int front, int side) {
        position = position.withFrontOffset(-front).withSideOffset(side);
    }

    public void rotateLadyLeft() {
        switch (position.getLadyDir()) {
        case ON_LINE:
            position = position.withLadyDir(LadyDir.SIDE);
            offset = offset.withLadyLineDir(!offset.isLadyLineDir());
            break;
        case SIDE:
            position = position.withLadyDir(LadyDir.ON_LINE);
            break;
        }

        // back rotate man
        rotateManRight();
        int front = position.getFrontOffset();
        int side = position.getSideOffset();
        position = position.withFrontOffset(side).withSideOffset(-front);
    }

    public void rotateLadyRight() {
        switch (position.getLadyDir()) {
        case ON_LINE:
            position = position.withLadyDir(LadyDir.SIDE);
            break;
        case SIDE:
            position = position.withLadyDir(LadyDir.ON_LINE);
            offset = offset.withLadyLineDir(!offset.isLadyLineDir());
            break;
        }

        // back rotate man
        rotateManLeft();
        int front = position.getFrontOffset();
        int side = position.getSideOffset();
        position = position.withFrontOffset(-side).withSideOffset(front);
    }

    public void rotateManLeft() {
        switch (position.getManDir()) {
        case OPPOSITE:
            position = position.withManDir(ManDir.RIGHT);
            break;
        case RIGHT:
            position = position.withManDir(ManDir.SAME);
            break;
        case SAME:
            position = position.withManDir(ManDir.LEFT);
            break;
        case LEFT:
            position = position.withManDir(ManDir.OPPOSITE);
            break;
        }
    }

    public void rotateManRight() {
        switch (position.getManDir()) {
        case OPPOSITE:
            position = position.withManDir(ManDir.LEFT);
            break;
        case RIGHT:
            position = position.withManDir(ManDir.OPPOSITE);
            break;
        case SAME:
            position = position.withManDir(ManDir.RIGHT);
            break;
        case LEFT:
            position = position.withManDir(ManDir.SAME);
            break;
        }
    }
}
