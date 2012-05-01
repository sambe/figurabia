/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 19.07.2009
 */
package figurabia.domain;

import java.io.Serializable;

/**
 * Immutable class representing a position of the dancers in Salsa Puertoriqueña.
 * 
 * @author Samuel Berner
 */
public class PuertoPosition implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -4191726723834447006L;

    public enum LadyDir {
        /**
         * The lady stands in direction of the line.
         */
        ON_LINE,
                /**
         * The lady stands sideways to the line (the line goes from the lady's right to left).
         */
        SIDE;
    }

    public enum ManDir {
        /**
         * The man looks in the opposite direction as the lady.
         */
        OPPOSITE(0),
                /**
         * The man looks in the same direction as the lady.
         */
        SAME(2),
                /**
         * The man looks in the direction left of the lady.
         */
        LEFT(1),
                /**
         * The man looks in the direction right of the lady.
         */
        RIGHT(3);

        private ManDir(int dir) {
            this.dir = dir;
        }

        public final int dir;
    }

    public enum HandHeight {
        /**
         * The hands are normally connected between man and lady (freely in the air).
         */
        NORMAL,
                /**
         * The hand is on the shoulder of the lady.
         */
        ON_SHOULDER,
                /**
         * The hand is on the hip of the lady.
         */
        ON_HIP,
                /**
         * The hands are joined, locked on hip-height.
         */
        HIP_LOCKED,
                /**
         * The hands are joined, locked on neck-height.
         */
        NECK_LOCKED,
                /**
         * The hands are joined, man's elbow on inner side of lady's arm.
         */
        ELBOW_LOCKED,
                /**
         * The arm in a right angle, hand point upwards and man and lady's lower arms touching.
         */
        HAND_LOCKED;
    }

    public enum HandJoint {
        /**
         * No hands joined.
         */
        FREE,
                /**
         * Both hands joined (left to right and right to left).
         */
        BOTH_JOINED,
                /**
         * Only the man's left hand joined with the lady's right.
         */
        LEFT_JOINED,
                /**
         * Only the man's right hand joined with the lady's left.
         */
        RIGHT_JOINED,
                /**
         * The man's right hand joined to the lady's right and the two left ones joined above the right ones.
         */
        BOTH_GREET,
                /**
         * Only the man's left hand joined to the lady's left.
         */
        LEFT_GREET,
                /**
         * Only the mans's right hand joined to the lady's right.
         */
        RIGHT_GREET;
    }

    public enum ArmWrapped {
        UNWRAPPED,
        HALF_WRAPPED,
        FULLY_WRAPPED;
    }

    // beat
    /**
     * either 1 or 5
     */
    private final int beat;// = 1;

    // position & direction
    private final LadyDir ladyDir;// = LadyDir.ON_LINE;
    private final ManDir manDir;// = ManDir.OPPOSITE;
    /**
     * 10 = normal position in front of the lady<br>
     * 0 = normal position when beside the lady<br>
     * -10 = normal position behind the lady
     */
    private final int frontOffset;// = 10;
    /**
     * -10 = normal position when left of the lady<br>
     * 0 = normal position when in front or behind the lady<br>
     * 10 = normal position when right of the lady
     */
    private final int sideOffset;// = 0;

    // hands
    private final HandJoint handsJoined;// = HandJoint.FREE;
    private final int handsTwist;// = 0;
    private final HandHeight leftHandHeight;// = HandHeight.NORMAL;
    private final HandHeight rightHandHeight;// = HandHeight.NORMAL;
    private final ArmWrapped leftArmAroundMan;// = false;
    private final ArmWrapped rightArmAroundMan;// = false;
    private final ArmWrapped leftArmAroundLady;// = false; // left as seen from the man
    private final ArmWrapped rightArmAroundLady;// = false; // right as seen from the man    

    public PuertoPosition(int beat, LadyDir ladyDir, ManDir manDir, int frontOffset, int sideOffset,
            HandJoint handsJoined, int handsTwist, HandHeight leftHandHeight, HandHeight rightHandHeight,
            ArmWrapped mansLeftArmAroundBody, ArmWrapped mansRightArmAroundBody, ArmWrapped ladysLeftArmAroundBody,
            ArmWrapped ladysRightArmAroundBody) {
        super();
        if (ladyDir == null) {
            throw new IllegalArgumentException("ladyDir must not be null");
        }
        if (manDir == null) {
            throw new IllegalArgumentException("manDir must not be null");
        }
        if (handsJoined == null) {
            throw new IllegalArgumentException("handsJoined must not be null");
        }
        if (leftHandHeight == null) {
            throw new IllegalArgumentException("leftHandHeight must not be null");
        }
        if (rightHandHeight == null) {
            throw new IllegalArgumentException("rightHandHeight must not be null");
        }
        this.beat = beat;
        this.ladyDir = ladyDir;
        this.manDir = manDir;
        this.frontOffset = frontOffset;
        this.sideOffset = sideOffset;
        this.handsJoined = handsJoined;
        this.handsTwist = handsTwist;
        this.leftHandHeight = leftHandHeight;
        this.rightHandHeight = rightHandHeight;
        this.leftArmAroundMan = mansLeftArmAroundBody;
        this.rightArmAroundMan = mansRightArmAroundBody;
        this.leftArmAroundLady = ladysLeftArmAroundBody;
        this.rightArmAroundLady = ladysRightArmAroundBody;
    }

    private static PuertoPosition INITIAL_POSITION = new PuertoPosition(1, LadyDir.ON_LINE, ManDir.OPPOSITE, 10, 0,
            HandJoint.FREE, 0, HandHeight.NORMAL, HandHeight.NORMAL, ArmWrapped.UNWRAPPED, ArmWrapped.UNWRAPPED,
            ArmWrapped.UNWRAPPED, ArmWrapped.UNWRAPPED);

    public int asNumber() {
        int num = 0;
        // 2=1bit for beat (1 or 5)
        num += beat == 5 ? 1 : 0;
        // 2=1bit for ladyDir (ON_LINE or SIDE)
        num <<= 1;
        num += ladyDir.ordinal();
        // 4=2bit for manDir (OPPOSITE, SAME, LEFT, RIGHT)
        num <<= 2;
        num += manDir.ordinal();
        // 7=3bit for frontOffset (-15, -10, -5, 0, 5, 10, 15)
        num <<= 3;
        num += (frontOffset + 15) / 5;
        // 7=3bit for sideOffset (maybe less possible)
        num <<= 3;
        num += (sideOffset + 15) / 5;
        // 8=3bit for handsJoined
        num <<= 3;
        num += handsJoined.ordinal();
        // 4=2bit for handsTwist (-1,0,1,2)
        num <<= 2;
        num += handsTwist + 1;
        // 7=3bit for leftHandHeight
        num <<= 3;
        num += leftHandHeight.ordinal();
        // 7=3bit for rightHandHeight
        num <<= 3;
        num += rightHandHeight.ordinal();
        // 4*3=4*2bit for ...AroundBody
        num <<= 2;
        num += leftArmAroundMan.ordinal();
        num <<= 2;
        num += rightArmAroundMan.ordinal();
        num <<= 2;
        num += leftArmAroundLady.ordinal();
        num <<= 2;
        num += rightArmAroundLady.ordinal();

        //totally: 29 bit
        return num;
    }

    /**
     * @return the beat
     */
    public int getBeat() {
        return beat;
    }

    /**
     * @param beat the beat to set
     */
    public PuertoPosition withBeat(int beat) {
        if (beat != 1 && beat != 5)
            throw new IllegalArgumentException("beat must be 1 or 5");
        return new PuertoPosition(beat, ladyDir, manDir, frontOffset, sideOffset, handsJoined, handsTwist,
                leftHandHeight, rightHandHeight, leftArmAroundMan, rightArmAroundMan, leftArmAroundLady,
                rightArmAroundLady);
    }

    /**
     * @return the ladyDir
     */
    public LadyDir getLadyDir() {
        return ladyDir;
    }

    /**
     * @param ladyDir the ladyDir to set
     */
    public PuertoPosition withLadyDir(LadyDir ladyDir) {
        return new PuertoPosition(beat, ladyDir, manDir, frontOffset, sideOffset, handsJoined, handsTwist,
                leftHandHeight, rightHandHeight, leftArmAroundMan, rightArmAroundMan, leftArmAroundLady,
                rightArmAroundLady);
    }

    /**
     * @return the manDir
     */
    public ManDir getManDir() {
        return manDir;
    }

    /**
     * @param manDir the manDir to set
     */
    public PuertoPosition withManDir(ManDir manDir) {
        return new PuertoPosition(beat, ladyDir, manDir, frontOffset, sideOffset, handsJoined, handsTwist,
                leftHandHeight, rightHandHeight, leftArmAroundMan, rightArmAroundMan, leftArmAroundLady,
                rightArmAroundLady);
    }

    /**
     * @return the frontOffset
     */
    public int getFrontOffset() {
        return frontOffset;
    }

    /**
     * @param frontOffset the frontOffset to set
     */
    public PuertoPosition withFrontOffset(int frontOffset) {
        if (frontOffset % 5 != 0 || frontOffset < -15 || frontOffset > 15)
            throw new IllegalArgumentException("frontOffset must be one of -15, -10, -5, 0, 5, 10, 15");
        return new PuertoPosition(beat, ladyDir, manDir, frontOffset, sideOffset, handsJoined, handsTwist,
                leftHandHeight, rightHandHeight, leftArmAroundMan, rightArmAroundMan, leftArmAroundLady,
                rightArmAroundLady);
    }

    /**
     * @return the sideOffset
     */
    public int getSideOffset() {
        return sideOffset;
    }

    /**
     * @param sideOffset the sideOffset to set
     */
    public PuertoPosition withSideOffset(int sideOffset) {
        if (sideOffset % 5 != 0 || sideOffset < -15 || sideOffset > 15)
            throw new IllegalArgumentException("sideOffset must be one of -15, -10, -5, 0, 5, 10, 15");
        return new PuertoPosition(beat, ladyDir, manDir, frontOffset, sideOffset, handsJoined, handsTwist,
                leftHandHeight, rightHandHeight, leftArmAroundMan, rightArmAroundMan, leftArmAroundLady,
                rightArmAroundLady);
    }

    /**
     * @return the handsJoined
     */
    public HandJoint getHandsJoined() {
        return handsJoined;
    }

    /**
     * @param handsJoined the handsJoined to set
     */
    public PuertoPosition withHandsJoined(HandJoint handsJoined) {
        return new PuertoPosition(beat, ladyDir, manDir, frontOffset, sideOffset, handsJoined, handsTwist,
                leftHandHeight, rightHandHeight, leftArmAroundMan, rightArmAroundMan, leftArmAroundLady,
                rightArmAroundLady);
    }

    /**
     * @return the handsTwist
     */
    public int getHandsTwist() {
        return handsTwist;
    }

    /**
     * @param handsTwist the handsTwist to set
     */
    public PuertoPosition withHandsTwist(int handsTwist) {
        return new PuertoPosition(beat, ladyDir, manDir, frontOffset, sideOffset, handsJoined, handsTwist,
                leftHandHeight, rightHandHeight, leftArmAroundMan, rightArmAroundMan, leftArmAroundLady,
                rightArmAroundLady);
    }

    /**
     * @return the leftHandHeight
     */
    public HandHeight getLeftHandHeight() {
        return leftHandHeight;
    }

    /**
     * @param leftHandHeight the leftHandHeight to set
     */
    public PuertoPosition withLeftHandHeight(HandHeight leftHandHeight) {
        return new PuertoPosition(beat, ladyDir, manDir, frontOffset, sideOffset, handsJoined, handsTwist,
                leftHandHeight, rightHandHeight, leftArmAroundMan, rightArmAroundMan, leftArmAroundLady,
                rightArmAroundLady);
    }

    /**
     * @return the rightHandHeight
     */
    public HandHeight getRightHandHeight() {
        return rightHandHeight;
    }

    /**
     * @param rightHandHeight the rightHandHeight to set
     */
    public PuertoPosition withRightHandHeight(HandHeight rightHandHeight) {
        return new PuertoPosition(beat, ladyDir, manDir, frontOffset, sideOffset, handsJoined, handsTwist,
                leftHandHeight, rightHandHeight, leftArmAroundMan, rightArmAroundMan, leftArmAroundLady,
                rightArmAroundLady);
    }

    /**
     * @return the leftArmAroundMan
     */
    public ArmWrapped getLeftArmAroundMan() {
        return leftArmAroundMan;
    }

    /**
     * @param leftArmAroundMan the leftArmAroundMan to set
     */
    public PuertoPosition withLeftArmAroundMan(ArmWrapped leftArmAroundMan) {
        return new PuertoPosition(beat, ladyDir, manDir, frontOffset, sideOffset, handsJoined, handsTwist,
                leftHandHeight, rightHandHeight, leftArmAroundMan, rightArmAroundMan, leftArmAroundLady,
                rightArmAroundLady);
    }

    /**
     * @return the rightArmAroundMan
     */
    public ArmWrapped getRightArmAroundMan() {
        return rightArmAroundMan;
    }

    /**
     * @param rightArmAroundMan the mansRightArmAroundBody to set
     */
    public PuertoPosition withRightArmAroundMan(ArmWrapped rightArmAroundMan) {
        return new PuertoPosition(beat, ladyDir, manDir, frontOffset, sideOffset, handsJoined, handsTwist,
                leftHandHeight, rightHandHeight, leftArmAroundMan, rightArmAroundMan, leftArmAroundLady,
                rightArmAroundLady);
    }

    /**
     * @return the leftArmAroundLady
     */
    public ArmWrapped getLeftArmAroundLady() {
        return leftArmAroundLady;
    }

    /**
     * @param leftArmAroundLady the leftArmAroundLady to set
     */
    public PuertoPosition withLeftArmAroundLady(ArmWrapped leftArmAroundLady) {
        return new PuertoPosition(beat, ladyDir, manDir, frontOffset, sideOffset, handsJoined, handsTwist,
                leftHandHeight, rightHandHeight, leftArmAroundMan, rightArmAroundMan, leftArmAroundLady,
                rightArmAroundLady);
    }

    /**
     * @return the rightArmAroundLady
     */
    public ArmWrapped getRightArmAroundLady() {
        return rightArmAroundLady;
    }

    /**
     * @param rightArmAroundLady the rightArmAroundLady to set
     */
    public PuertoPosition withRightArmAroundLady(ArmWrapped rightArmAroundLady) {
        return new PuertoPosition(beat, ladyDir, manDir, frontOffset, sideOffset, handsJoined, handsTwist,
                leftHandHeight, rightHandHeight, leftArmAroundMan, rightArmAroundMan, leftArmAroundLady,
                rightArmAroundLady);
    }

    @Override
    public int hashCode() {
        return asNumber();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PuertoPosition) {
            PuertoPosition p = (PuertoPosition) obj;
            return asNumber() == p.asNumber();
        }
        return false;
    }

    public static PuertoPosition getInitialPosition() {
        return INITIAL_POSITION;
    }

    /**
     * Changes the beat to the other state (1 or 5 depending on what was set before).
     */
    public PuertoPosition invertBeat() {
        return withBeat(beat == 1 ? 5 : 1);
    }
}
