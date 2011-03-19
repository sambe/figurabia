/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.05.2009
 */
package figurabia.experiment.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class SpectralTableViewer extends JPanel {

    private final static short BLACK_THRESHOLD = 1 << 14;
    private final static short BLUE_THRESHOLD = 1 << 13;
    private final static short GREEN_THRESHOLD = 1 << 9;
    private final static short YELLOW_THRESHOLD = 0;

    private final static int MARGIN_LEFT = 20;
    private final static int MARGIN_RIGHT = 20;
    private final static int MARGIN_BOTTOM = 20;
    private final static int MARGIN_TOP = 20;

    private short[][] spectralTable;

    private int fps = 0;

    @Override
    public void paint(Graphics g) {
        Dimension dim = getSize();

        // draw spectral table
        for (int i = 0; i < spectralTable.length; i++) {
            for (int j = 0; j < spectralTable[i].length; j++) {
                drawRect(g, i, j, dim);
            }
        }

        if (fps != 0) {
            // draw ruler
            int rulerHeight = dim.height - MARGIN_BOTTOM;
            //int rulerLength = dim.width - MARGIN_LEFT - MARGIN_RIGHT;
            g.setColor(Color.BLACK);
            g.drawLine(MARGIN_LEFT, rulerHeight, MARGIN_RIGHT, rulerHeight);
            int second = 0;
            for (int i = 0; i * fps < spectralTable.length; i++) {
                int xi = nrToX(i * fps, spectralTable.length, dim.width);
                g.drawLine(xi, MARGIN_TOP - 5, xi, rulerHeight + 5);
                if (i % 5 == 0)
                    g.drawString(Integer.toString(second), xi - 5, rulerHeight + 15);
                second++;
            }
        }
    }

    private void drawRect(Graphics g, int i, int j, Dimension dim) {
        int x1 = nrToX(i, spectralTable.length, dim.width);
        int y1 = specToY(j, spectralTable[0].length, dim.height);
        int x2 = nrToX(i + 1, spectralTable.length, dim.width);
        int y2 = specToY(j + 1, spectralTable[0].length, dim.height);
        Color color = valueToColor(spectralTable[i][j]);
        g.setColor(color);
        g.fillRect(x1, y1, x2 - x1, y2 - y1);
    }

    private int nrToX(int j, int n, int width) {
        return j * (width - MARGIN_LEFT - MARGIN_RIGHT) / n + MARGIN_LEFT;
    }

    private int specToY(int i, int n, int height) {
        return i * (height - MARGIN_TOP - MARGIN_BOTTOM) / n + MARGIN_TOP;
    }

    private Color valueToColor(short value) {
        if (value > BLACK_THRESHOLD)
            return Color.BLACK;
        if (value > BLUE_THRESHOLD)
            return Color.BLUE;
        if (value > GREEN_THRESHOLD)
            return Color.GREEN;
        if (value > YELLOW_THRESHOLD)
            return Color.YELLOW;
        return Color.WHITE;
    }

    /**
     * @param spectralTable the spectralTable to set
     */
    public void setSpectralTable(short[][] spectralTable) {
        this.spectralTable = spectralTable;
    }

    private Object waitCondition = new Object();
    private boolean keepWaiting = true;

    private void continueExecution() {
        synchronized (waitCondition) {
            keepWaiting = false;
            waitCondition.notifyAll();
        }
    }

    private void waitForContinue() {
        try {
            synchronized (waitCondition) {
                while (keepWaiting) {
                    waitCondition.wait();
                }
            }
        } catch (InterruptedException ex) {
            // continue execution because of interrupt
        }
    }

    public static void displayViewer(final short[][] spectralTable, int fps) {
        final SpectralTableViewer viewer = new SpectralTableViewer();
        viewer.setSpectralTable(spectralTable);
        viewer.setFps(fps);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Spectral Table Viewer");
                Container c = frame.getContentPane();
                c.setLayout(new BorderLayout());
                c.add(viewer, BorderLayout.CENTER);

                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        viewer.continueExecution();
                    }
                });
                frame.setSize(1200, 800);
                frame.setVisible(true);
            }
        });

        viewer.waitForContinue();
    }

    /**
     * @param fps the fps to set
     */
    public void setFps(int fps) {
        this.fps = fps;
    }
}
