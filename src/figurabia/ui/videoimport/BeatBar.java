/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 18.07.2009
 */
package figurabia.ui.videoimport;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Polygon;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import figurabia.ui.util.SimplePanelFrame;

@SuppressWarnings("serial")
public class BeatBar extends JComponent {

    private final static long MOVE_DELTA = 1000000000L / 15L;

    private final static int MAX_SELECTION_TOLERANCE = 5;
    private final static int MARGIN = 20;
    private final static int RADIUS = 5;

    private List<SelectionChangedListener> listeners = new ArrayList<SelectionChangedListener>();
    private int selectedBeatType = -1;
    private long selectedBeatTime = -1;
    private Polygon leftArrow;
    private Polygon rightArrow;
    private boolean lastClickMoved = false;

    // model data
    private long videoLength;
    private List<Long> beatsOn1 = new ArrayList<Long>();
    private List<Long> beatsOn5 = new ArrayList<Long>();

    public BeatBar() {
        addMouseListener(new BeatBarMouseListener());
        setMinimumSize(new Dimension(100, 40));
        setPreferredSize(new Dimension(400, 40));
    }

    /**
     * Used for notification when the selection of a {@link BeatBar} changed.
     */
    public interface SelectionChangedListener {
        /**
         * Notifies that the selection of the {@link BeatBar} changed.
         */
        void selectionChanged();
    }

    public void addSelectionChangedListener(SelectionChangedListener listener) {
        listeners.add(listener);
    }

    public void removeSelectionChangedListener(SelectionChangedListener listener) {
        listeners.remove(listener);
    }

    protected void notifySelectionChangedListeners() {
        for (int i = 0; i < listeners.size(); i++) {
            try {
                listeners.get(i).selectionChanged();
            } catch (RuntimeException e) {
                // we want to protect this component from runtime exceptions thrown in listeners.
                e.printStackTrace();
            }
        }
    }

    private class BeatBarMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            lastClickMoved = false;
            // if one is already selected, find out first, whether the click is in one of the buttons
            if (selectedBeatType != -1 && leftArrow != null && rightArrow != null) {
                if (leftArrow.contains(e.getPoint())) {
                    moveSelectedBeat(selectedBeatTime - MOVE_DELTA);
                    lastClickMoved = true;
                    return;
                }
                if (rightArrow.contains(e.getPoint())) {
                    moveSelectedBeat(selectedBeatTime + MOVE_DELTA);
                    lastClickMoved = true;
                    return;
                }
            }

            // determine the nearest beat and if it is near enough to be a valid select
            int minDist = Integer.MAX_VALUE;
            int beatType = -1;
            long beatTime = -1;
            if (Math.abs(e.getY() - getHeight() / 2) <= MAX_SELECTION_TOLERANCE) {
                for (int i = 0; i < beatsOn1.size(); i++) {
                    // calculate screen position
                    int screenX = calcScreenPosX(beatsOn1.get(i));
                    int pixelDist = Math.abs(screenX - e.getX());
                    if (pixelDist <= MAX_SELECTION_TOLERANCE && pixelDist < minDist) {
                        minDist = pixelDist;
                        beatType = 1;
                        beatTime = beatsOn1.get(i);
                    }
                }
                for (int i = 0; i < beatsOn5.size(); i++) {
                    // calculate screen position
                    int screenX = calcScreenPosX(beatsOn5.get(i));
                    int pixelDist = Math.abs(screenX - e.getX());
                    if (pixelDist <= MAX_SELECTION_TOLERANCE && pixelDist < minDist) {
                        minDist = pixelDist;
                        beatType = 5;
                        beatTime = beatsOn5.get(i);
                    }
                }
            }

            // select or unselect a beat (depending on wheter one has been found before)
            selectBeat(beatType, beatTime);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && SwingUtilities.isRightMouseButton(e)) {
                if (!lastClickMoved) {
                    deleteSelectedBeat();
                }
            }
        }
    }

    public void selectBeat(int beatType, long beatTime) {
        if (beatType == 1 && beatsOn1.contains(beatTime) || beatType == 5 && beatsOn5.contains(beatTime)) {

            // notify listeners only if selection changed
            if (selectedBeatType != beatType || selectedBeatTime != beatTime) {
                selectedBeatType = beatType;
                selectedBeatTime = beatTime;
                repaint();
                notifySelectionChangedListeners();
            }
        } else {
            // notify listeners only if selection changed
            if (selectedBeatType != -1) {
                selectedBeatType = -1;
                selectedBeatTime = -1;
                repaint();
                notifySelectionChangedListeners();
            }
        }
    }

    public void moveSelectedBeat(long newBeatTime) {
        if (selectedBeatType == 1 && beatsOn1.contains(selectedBeatTime)) {
            beatsOn1.remove(selectedBeatTime);
            beatsOn1.add(newBeatTime);
            Collections.sort(beatsOn1);
        } else if (selectedBeatType == 5 && beatsOn5.contains(selectedBeatTime)) {
            beatsOn5.remove(selectedBeatTime);
            beatsOn5.add(newBeatTime);
            Collections.sort(beatsOn5);
        } else
            throw new IllegalStateException("The selected beat does not exist and therefore cannot be moved.");
        selectBeat(selectedBeatType, newBeatTime);
    }

    public int calcScreenPosX(long beatTime) {
        int width = getWidth() - 2 * MARGIN;
        return (int) (beatTime * width / videoLength) + MARGIN;
    }

    @Override
    public void paint(Graphics g) {
        g.clearRect(0, 0, getWidth(), getHeight());

        int yMid = getHeight() / 2;

        // draw bar
        g.setColor(Color.DARK_GRAY);
        g.drawLine(MARGIN, yMid, getWidth() - MARGIN, yMid);
        g.drawLine(MARGIN, yMid - 5, MARGIN, yMid + 5);
        g.drawLine(getWidth() - MARGIN, yMid - 5, getWidth() - MARGIN, yMid + 5);

        // draw beats
        g.setColor(Color.BLUE);
        for (long beat : beatsOn5) {
            int x = calcScreenPosX(beat);
            g.fillOval(x - RADIUS, yMid - RADIUS, 2 * RADIUS, 2 * RADIUS);
        }
        g.setColor(Color.GREEN);
        for (long beat : beatsOn1) {
            int x = calcScreenPosX(beat);
            g.fillOval(x - RADIUS, yMid - RADIUS, 2 * RADIUS, 2 * RADIUS);
        }

        // mark selected beat
        if (selectedBeatType != -1) {
            g.setColor(Color.GRAY);
            int x = calcScreenPosX(selectedBeatTime);
            g.fillOval(x - RADIUS, yMid - RADIUS, 2 * RADIUS, 2 * RADIUS);
            g.setColor(Color.BLACK);
            g.drawOval(x - RADIUS - 2, yMid - RADIUS - 2, 2 * RADIUS + 4, 2 * RADIUS + 4);
            // draw an arrow left and right of the selected beat
            leftArrow = new Polygon(new int[] { x - RADIUS - 4, x - RADIUS - 4, x - RADIUS - 4 - 2 * RADIUS - 4 },
                    new int[] { yMid - RADIUS - 2, yMid + RADIUS + 2, yMid }, 3);
            g.fillPolygon(leftArrow);
            rightArrow = new Polygon(new int[] { x + RADIUS + 5, x + RADIUS + 5, x + RADIUS + 5 + 2 * RADIUS + 4 },
                    new int[] { yMid - RADIUS - 2, yMid + RADIUS + 2, yMid }, 3);
            g.fillPolygon(rightArrow);
        }
    }

    /**
     * @return the video length
     */
    public long getVideoLength() {
        return videoLength;
    }

    /**
     * @param length the video length to set
     */
    public void setVideoLength(long length) {
        this.videoLength = length;
    }

    /**
     * @return the beats1
     */
    public List<Long> getBeatsOn1() {
        return Collections.unmodifiableList(beatsOn1);
    }

    /**
     * @param beatsOn1 the beats1 to set
     */
    public void setBeatsOn1(List<Long> beatsOn1) {
        this.beatsOn1 = new ArrayList<Long>(beatsOn1);
        Collections.sort(this.beatsOn1);
        repaint();
    }

    /**
     * Adds a beat to the list of beats on 1.
     * 
     * @param beat the beat to add
     */
    public void addBeatOn1(long beat) {
        beatsOn1.add(beat);
        Collections.sort(beatsOn1);
        repaint();
    }

    /**
     * @return the beats5
     */
    public List<Long> getBeatsOn5() {
        return Collections.unmodifiableList(beatsOn5);
    }

    /**
     * @param beatsOn5 the beats5 to set
     */
    public void setBeatsOn5(List<Long> beatsOn5) {
        this.beatsOn5 = new ArrayList<Long>(beatsOn5);
        Collections.sort(this.beatsOn5);
        repaint();
    }

    /**
     * Adds a beat to the list of beats on 5.
     * 
     * @param beat the beat to add
     */
    public void addBeatOn5(long beat) {
        beatsOn5.add(beat);
        Collections.sort(beatsOn5);
        repaint();
    }

    public List<Long> getAllBeats() {
        List<Long> beats = new ArrayList<Long>();
        beats.addAll(beatsOn1);
        beats.addAll(beatsOn5);
        Collections.sort(beats);
        return beats;
    }

    /**
     * Deletes the selected beat.
     */
    public void deleteSelectedBeat() {
        if (selectedBeatType == 1) {
            beatsOn1.remove(selectedBeatTime);
        } else if (selectedBeatType == 5) {
            beatsOn5.remove(selectedBeatTime);
        }
        selectBeat(-1, -1);
    }

    /**
     * @return the selectedBeatType
     */
    public int getSelectedBeatType() {
        return selectedBeatType;
    }

    /**
     * @return the selectedBeatTime
     */
    public long getSelectedBeatTime() {
        return selectedBeatTime;
    }

    /**
     * This is only for testing.
     */
    public static void main(String[] args) {
        BeatBar bb = new BeatBar();
        bb.setVideoLength(20000000000L);
        bb.setBeatsOn1(Arrays.asList(15000000000L, 10000000000L, 12000000000L));
        bb.setBeatsOn5(Arrays.asList(11000000000L, 12600000000L, 17000000000L));
        new SimplePanelFrame(bb, 300, 50);
    }
}
