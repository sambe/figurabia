/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 23.07.2009
 */
package figurabia.ui.positionviewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import net.miginfocom.swing.MigLayout;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.domain.PuertoPosition.ArmWrapped;
import figurabia.domain.PuertoPosition.HandHeight;
import figurabia.domain.PuertoPosition.HandJoint;
import figurabia.ui.framework.PositionListener;

@SuppressWarnings("serial")
public class PositionDialogEditor extends JPanel {

    private JPanel positionPanel;
    private PositionViewer positionViewer;
    private HandsPanel handsPanel;
    private LeftRightPanel leftHandPanel;
    private LeftRightPanel rightHandPanel;

    private JButton copyToNext;

    private List<PositionListener> positionListeners = new ArrayList<PositionListener>();

    public PositionDialogEditor() {
        this(true);
    }

    public PositionDialogEditor(boolean editorMode) {
        setLayout(new MigLayout("", "[][]", ""));

        // position dialog part
        positionPanel = new JPanel();
        positionPanel.setBorder(BorderFactory.createTitledBorder("Position"));
        positionPanel.setLayout(new MigLayout("", "", "[140:280][]push"));
        add(positionPanel, "span,wrap");

        // base values panel (beat, directions and offsets)
        positionViewer = new PositionViewer();
        positionViewer.setEditable(true);
        positionPanel.add(positionViewer, "span,grow,push,wrap");

        // hands panel
        handsPanel = new HandsPanel();
        positionPanel.add(handsPanel);

        // copy to next button
        if (editorMode) {
            copyToNext = new JButton("Copy to Next");
            copyToNext.setEnabled(false);
            add(copyToNext, "span,gapbefore,align right");
        } else {
            positionViewer.setBeatChangeable(true);
        }

        // propagate position listener events
        positionViewer.addPositionListener(new PositionListener() {
            @Override
            public void positionActive(PuertoPosition p, PuertoOffset offset, PuertoOffset offsetChange) {
                assignPosition(p);
                updatePositionListeners(p, offset, offsetChange);
            }
        });
    }

    private class HandsPanel extends JPanel {

        private JComboBox handsJoinedComboBox;
        private JRadioButton twistRadioButton9;
        private JRadioButton twistRadioButton0;
        private JRadioButton twistRadioButton1;
        private JRadioButton twistRadioButton2;
        private ButtonGroup twistRadioButtonGroup;

        public HandsPanel() {
            setBorder(BorderFactory.createTitledBorder("Hands"));
            setLayout(new MigLayout());

            // joined & twist
            add(new JLabel("Joined"));
            handsJoinedComboBox = new JComboBox(new String[] { "Free", "Both Joined", "Left Joined", "Right Joined",
                    "Both Greet", "Left Greet", "Right Greet" });
            add(handsJoinedComboBox, "wrap");
            handsJoinedComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setPosition(getPosition().withHandsJoined(getHandsJoined()));
                }
            });

            add(new JLabel("Twist"));
            twistRadioButton9 = new JRadioButton("-1");
            twistRadioButton0 = new JRadioButton("0");
            twistRadioButton1 = new JRadioButton("1");
            twistRadioButton2 = new JRadioButton("2");
            twistRadioButtonGroup = new ButtonGroup();
            twistRadioButtonGroup.add(twistRadioButton9);
            twistRadioButtonGroup.add(twistRadioButton0);
            twistRadioButtonGroup.add(twistRadioButton1);
            twistRadioButtonGroup.add(twistRadioButton2);
            twistRadioButton0.setSelected(true);
            add(twistRadioButton9, "split 4");
            add(twistRadioButton0);
            add(twistRadioButton1);
            add(twistRadioButton2, "wrap");
            ActionListener twistActionListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setPosition(getPosition().withHandsTwist(getHandsTwist()));
                }
            };
            twistRadioButton9.addActionListener(twistActionListener);
            twistRadioButton0.addActionListener(twistActionListener);
            twistRadioButton1.addActionListener(twistActionListener);
            twistRadioButton2.addActionListener(twistActionListener);

            // left & right hand panels
            leftHandPanel = new LeftRightPanel("Left (Man)");
            add(leftHandPanel, "span, split2");
            leftHandPanel.heightComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setPosition(getPosition().withLeftHandHeight(getLeftHandHeight()));
                }
            });
            leftHandPanel.aroundManComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setPosition(getPosition().withLeftArmAroundMan(getLeftArmAroundMan()));
                }
            });
            leftHandPanel.aroundLadyComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setPosition(getPosition().withLeftArmAroundLady(getLeftArmAroundLady()));
                }
            });
            rightHandPanel = new LeftRightPanel("Right (Man)");
            add(rightHandPanel);
            rightHandPanel.heightComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setPosition(getPosition().withRightHandHeight(getRightHandHeight()));
                }
            });
            rightHandPanel.aroundManComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setPosition(getPosition().withRightArmAroundMan(getRightArmAroundMan()));
                }
            });
            rightHandPanel.aroundLadyComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setPosition(getPosition().withRightArmAroundLady(getRightArmAroundLady()));
                }
            });
        }

        public void setTwistEnabled(boolean enabled) {
            twistRadioButton0.setEnabled(enabled);
            twistRadioButton1.setEnabled(enabled);
            twistRadioButton2.setEnabled(enabled);
            twistRadioButton9.setEnabled(enabled);
        }
    }

    private class LeftRightPanel extends JPanel {

        private JComboBox heightComboBox;
        private JComboBox aroundLadyComboBox;
        private JComboBox aroundManComboBox;

        public LeftRightPanel(String name) {
            setBorder(BorderFactory.createTitledBorder(name));
            setLayout(new MigLayout());

            //add(new JLabel("Height"));
            heightComboBox = new JComboBox(new String[] { "Normal", "On Shoulder", "On Hip", "Hip Locked",
                    "Neck Locked", "Elbow Locked", "Hand Locked" });
            //heightComboBox.setSize(50, heightComboBox.getHeight());
            add(heightComboBox, "width 125px!,wrap");

            aroundLadyComboBox = new JComboBox(new String[] { "Not Around Lady", "1/2 Around Lady", "1/1 Around Lady" });
            add(aroundLadyComboBox, "span 2, width 125px!,wrap");

            aroundManComboBox = new JComboBox(new String[] { "Not Around Man", "1/2 Around Man", "1/1 Around Man" });
            add(aroundManComboBox, "span 2, width 125px!");
        }

        public void setAllEnabled(boolean enabled) {
            heightComboBox.setEnabled(enabled);
            aroundLadyComboBox.setEnabled(enabled);
            aroundManComboBox.setEnabled(enabled);
        }
    }

    /**
     * Sets all the values of a {@link PuertoPosition} into the UI controls.
     * 
     * @param p the position
     */
    public void setPosition(PuertoPosition p) {
        if (!p.equals(positionViewer.getPosition())) {
            p = updateEnabledState(p);
            positionViewer.setPosition(p);
        }
    }

    private void assignPosition(PuertoPosition p) {
        setHandsJoined(p.getHandsJoined());
        setHandsTwist(p.getHandsTwist());
        setLeftHandHeight(p.getLeftHandHeight());
        setRightHandHeight(p.getRightHandHeight());
        setLeftArmAroundMan(p.getLeftArmAroundMan());
        setRightArmAroundMan(p.getRightArmAroundMan());
        setLeftArmAroundLady(p.getLeftArmAroundLady());
        setRightArmAroundLady(p.getRightArmAroundLady());
    }

    /**
     * Gets the current position.
     * 
     * @return the position
     */
    public PuertoPosition getPosition() {
        return positionViewer.getPosition();
    }

    /**
     * Sets the offset (modifiable part of the offset).
     * 
     * @param offset the offset (modifiable part of the offset)
     */
    public void setOffset(PuertoOffset offset) {
        positionViewer.setOffset(offset);
    }

    public PuertoOffset getOffset() {
        return positionViewer.getOffset();
    }

    /**
     * Applies the given offset to the offset that is set.
     * 
     * @param offsetsway
     */
    public void applyOffset(PuertoOffset offset) {
        positionViewer.setOffset(positionViewer.getOffset().addOffset(offset));
    }

    /**
     * Sets the base offset (non-modifiable part of the offset).
     * 
     * @param baseOffset the base offset (non-modifiable part of the offset)
     */
    public void setBaseOffset(PuertoOffset baseOffset) {
        positionViewer.setBaseOffset(baseOffset);
    }

    /**
     * @return the handsJoined
     */
    public HandJoint getHandsJoined() {
        return HandJoint.values()[handsPanel.handsJoinedComboBox.getSelectedIndex()];
    }

    /**
     * @param handsJoined the handsJoined to set
     */
    public void setHandsJoined(HandJoint handsJoined) {
        handsPanel.handsJoinedComboBox.setSelectedIndex(handsJoined.ordinal());
    }

    /**
     * @return the handsTwist
     */
    public int getHandsTwist() {
        if (handsPanel.twistRadioButton9.isSelected())
            return -1;
        if (handsPanel.twistRadioButton0.isSelected())
            return 0;
        if (handsPanel.twistRadioButton1.isSelected())
            return 1;
        if (handsPanel.twistRadioButton2.isSelected())
            return 2;
        throw new IllegalStateException("none is selected");
    }

    /**
     * @param handsTwist the handsTwist to set
     */
    public void setHandsTwist(int handsTwist) {
        if (handsTwist == -1)
            handsPanel.twistRadioButton9.setSelected(true);
        else if (handsTwist == 0)
            handsPanel.twistRadioButton0.setSelected(true);
        else if (handsTwist == 1)
            handsPanel.twistRadioButton1.setSelected(true);
        else if (handsTwist == 2)
            handsPanel.twistRadioButton2.setSelected(true);
        else
            throw new IllegalArgumentException("only values -1, 0, 1 and 2 allowed. Was " + handsTwist);
    }

    /**
     * @return the leftHandHeight
     */
    public HandHeight getLeftHandHeight() {
        return HandHeight.values()[leftHandPanel.heightComboBox.getSelectedIndex()];
    }

    /**
     * @param leftHandHeight the leftHandHeight to set
     */
    public void setLeftHandHeight(HandHeight leftHandHeight) {
        leftHandPanel.heightComboBox.setSelectedIndex(leftHandHeight.ordinal());
    }

    /**
     * @return the rightHandHeight
     */
    public HandHeight getRightHandHeight() {
        return HandHeight.values()[rightHandPanel.heightComboBox.getSelectedIndex()];
    }

    /**
     * @param rightHandHeight the rightHandHeight to set
     */
    public void setRightHandHeight(HandHeight rightHandHeight) {
        rightHandPanel.heightComboBox.setSelectedIndex(rightHandHeight.ordinal());
    }

    /**
     * @return the leftArmAroundMan
     */
    public ArmWrapped getLeftArmAroundMan() {
        return ArmWrapped.values()[leftHandPanel.aroundManComboBox.getSelectedIndex()];
    }

    /**
     * @param leftArmAroundMan the leftArmAroundMan to set
     */
    public void setLeftArmAroundMan(ArmWrapped leftArmAroundMan) {
        leftHandPanel.aroundManComboBox.setSelectedIndex(leftArmAroundMan.ordinal());
    }

    /**
     * @return the rightArmAroundMan
     */
    public ArmWrapped getRightArmAroundMan() {
        return ArmWrapped.values()[rightHandPanel.aroundManComboBox.getSelectedIndex()];
    }

    /**
     * @param rightArmAroundMan the rightArmAroundMan to set
     */
    public void setRightArmAroundMan(ArmWrapped rightArmAroundMan) {
        rightHandPanel.aroundManComboBox.setSelectedIndex(rightArmAroundMan.ordinal());
    }

    /**
     * @return the leftArmAroundLady
     */
    public ArmWrapped getLeftArmAroundLady() {
        return ArmWrapped.values()[leftHandPanel.aroundLadyComboBox.getSelectedIndex()];
    }

    /**
     * @param leftArmAroundLady the leftArmAroundLady to set
     */
    public void setLeftArmAroundLady(ArmWrapped leftArmAroundLady) {
        leftHandPanel.aroundLadyComboBox.setSelectedIndex(leftArmAroundLady.ordinal());
    }

    /**
     * @return the rightArmAroundLady
     */
    public ArmWrapped getRightArmAroundLady() {
        return ArmWrapped.values()[rightHandPanel.aroundLadyComboBox.getSelectedIndex()];
    }

    /**
     * @param rightArmAroundLady the rightArmAroundLady to set
     */
    public void setRightArmAroundLady(ArmWrapped rightArmAroundLady) {
        rightHandPanel.aroundLadyComboBox.setSelectedIndex(rightArmAroundLady.ordinal());
    }

    // idea: maybe split into "position pruning" (must be done just before setting) and "adjusting the GUI controls" (should be done as a listener)
    private PuertoPosition updateEnabledState(PuertoPosition position) {
        boolean leftEnabled = false, rightEnabled = false;
        switch (position.getHandsJoined()) {
        case FREE:
            leftEnabled = false;
            rightEnabled = false;
            break;
        case LEFT_GREET:
        case LEFT_JOINED:
            leftEnabled = true;
            rightEnabled = false;
            break;
        case RIGHT_GREET:
        case RIGHT_JOINED:
            leftEnabled = false;
            rightEnabled = true;
            break;
        case BOTH_GREET:
        case BOTH_JOINED:
            leftEnabled = true;
            rightEnabled = true;
            break;
        }

        if (!leftEnabled) {
            position = position.withLeftArmAroundLady(ArmWrapped.UNWRAPPED)
                    .withLeftArmAroundMan(ArmWrapped.UNWRAPPED)
                    .withLeftHandHeight(HandHeight.NORMAL);
        }
        leftHandPanel.setEnabled(leftEnabled);
        leftHandPanel.setAllEnabled(leftEnabled);
        if (!rightEnabled) {
            position = position.withRightArmAroundLady(ArmWrapped.UNWRAPPED)
                    .withRightArmAroundMan(ArmWrapped.UNWRAPPED)
                    .withRightHandHeight(HandHeight.NORMAL);
        }
        rightHandPanel.setEnabled(rightEnabled);
        rightHandPanel.setAllEnabled(rightEnabled);
        boolean twistEnabled = isOneOf(position, HandJoint.BOTH_GREET, HandJoint.BOTH_JOINED);
        if (!twistEnabled) {
            position = position.withHandsTwist(0);
        }
        handsPanel.setTwistEnabled(twistEnabled);
        // disabling the 2 is not correct because depending on how the dancers are oriented to each other
        // the 2 is a valid position or not
        /*if (position.getHandsJoined() == HandJoint.BOTH_JOINED) {
            if (position.getHandsTwist() == 2)
                position = position.withHandsTwist(1);
            handsPanel.twistRadioButton2.setEnabled(false);
        }*/

        return position;
    }

    private boolean isOneOf(PuertoPosition pos, HandJoint... joints) {
        HandJoint posHJ = pos.getHandsJoined();
        for (HandJoint j : joints) {
            if (posHJ == j)
                return true;
        }
        return false;
    }

    /**
     * @param l
     * @see javax.swing.AbstractButton#addActionListener(java.awt.event.ActionListener)
     */
    public void addCopyToNextActionListener(ActionListener l) {
        copyToNext.addActionListener(l);
    }

    /**
     * @param l
     * @see javax.swing.AbstractButton#removeActionListener(java.awt.event.ActionListener)
     */
    public void removeCopyToNextActionListener(ActionListener l) {
        copyToNext.removeActionListener(l);
    }

    /**
     * @param b
     * @see javax.swing.AbstractButton#setEnabled(boolean)
     */
    public void setCopyToNextEnabled(boolean b) {
        copyToNext.setEnabled(b);
    }

    public void addPositionListener(PositionListener l) {
        positionListeners.add(l);
    }

    public void removePositionListener(PositionListener l) {
        positionListeners.remove(l);
    }

    protected void updatePositionListeners(PuertoPosition p, PuertoOffset offset, PuertoOffset offsetChange) {
        for (PositionListener l : positionListeners) {
            try {
                l.positionActive(p, offset, offsetChange);
            } catch (RuntimeException e) {
                // catch exceptions here to avoid unnecessary effects
                System.err.println("Exception from a PositionListener. Position: " + p);
                e.printStackTrace();
            }
        }
    }

    public void startCollectingUpdates() {
        positionViewer.startCollectingUpdates();
    }

    public void finishCollectingUpdates() {
        positionViewer.finishCollectingUpdates();
    }
}
