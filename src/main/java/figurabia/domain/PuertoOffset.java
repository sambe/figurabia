/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 20.07.2009
 */
package figurabia.domain;

import java.io.Serializable;

/**
 * Together with a {@link PuertoPosition} object this defines an absolute position of a pair of dancers in space. Or
 * alternatively, the delta between two absolute positions.
 * <p>
 * This object is designed to be immutable (all fields are final).
 * 
 * @author Samuel Berner
 */
public class PuertoOffset implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -4082352712517796820L;
    /**
     * the direction of lady to the direction of the line.
     */
    private final boolean ladyLineDir;
    /**
     * absolute position in line direction (does not change when ladyLineDir changes)
     */
    private final int absPosLineDir;
    /**
     * absolute position sideways of the line.
     */
    private final int absPosSideDir;

    public PuertoOffset(boolean ladyLineDir, int absPosLineDir, int absPosSideDir) {
        super();
        this.ladyLineDir = ladyLineDir;
        this.absPosLineDir = absPosLineDir;
        this.absPosSideDir = absPosSideDir;
    }

    private final static PuertoOffset INITIAL_OFFSET = new PuertoOffset(false, 0, 0);

    private static PuertoOffset create(boolean ladyLineDir, int absPosLineDir, int absPosSideDir) {
        return new PuertoOffset(ladyLineDir, absPosLineDir, absPosSideDir);
    }

    /**
     * @return the ladyLineDir
     */
    public boolean isLadyLineDir() {
        return ladyLineDir;
    }

    /**
     * @param ladyLineDir the ladyLineDir to set
     */
    public PuertoOffset withLadyLineDir(boolean ladyLineDir) {
        return create(ladyLineDir, absPosLineDir, absPosSideDir);
    }

    /**
     * @return the absPosLineDir
     */
    public int getAbsPosLineDir() {
        return absPosLineDir;
    }

    /**
     * @param absPosLineDir the absPosLineDir to set
     */
    public PuertoOffset withAbsPosLineDir(int absPosLineDir) {
        return create(ladyLineDir, absPosLineDir, absPosSideDir);
    }

    /**
     * @return the absPosSideDir
     */
    public int getAbsPosSideDir() {
        return absPosSideDir;
    }

    /**
     * @param absPosSideDir the absPosSideDir to set
     */
    public PuertoOffset withAbsPosSideDir(int absPosSideDir) {
        return create(ladyLineDir, absPosLineDir, absPosSideDir);
    }

    /**
     * Adds another offset to this one.
     * 
     * @param offset
     */
    public PuertoOffset addOffset(PuertoOffset offset) {
        int absPosLineDir = this.absPosLineDir + (ladyLineDir ? -1 : 1) * offset.absPosLineDir;
        int absPosSideDir = this.absPosSideDir + (ladyLineDir ? -1 : 1) * offset.absPosSideDir;
        boolean ladyLineDir = this.ladyLineDir;
        if (offset.ladyLineDir)
            ladyLineDir = !ladyLineDir;
        return create(ladyLineDir, absPosLineDir, absPosSideDir);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PuertoOffset) {
            PuertoOffset po = (PuertoOffset) obj;
            return ladyLineDir == po.ladyLineDir && absPosLineDir == po.absPosLineDir
                    && absPosSideDir == po.absPosSideDir;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (ladyLineDir ? 17 : 0) + 41 * (absPosLineDir + 41 * absPosSideDir);
    }

    public static PuertoOffset getInitialOffset() {
        return INITIAL_OFFSET;
    }
}
