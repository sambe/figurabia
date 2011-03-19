/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 24.05.2009
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
public class SoundViewer extends JPanel {

    private final static int MARGIN_LEFT = 20;
    private final static int MARGIN_RIGHT = 20;
    private final static int MARGIN_BOTTOM = 20;
    private final static int MARGIN_TOP = 20;

    private short[] values;

    private int fps = 0;

    @Override
    public void paint(Graphics g) {

        Dimension dim = getSize();

        g.setColor(Color.DARK_GRAY);
        g.drawLine(0, dim.height / 2, dim.width, dim.height / 2);

        int n = values.length;
        g.setColor(Color.BLACK);
        for (int i = 1; i < n; i++) {
            drawLine(g, i - 1, n, values[i - 1], values[i], dim);
        }

        if (fps != 0) {
            // draw ruler
            int rulerHeight = dim.height - MARGIN_BOTTOM;
            //int rulerLength = dim.width - MARGIN_LEFT - MARGIN_RIGHT;
            g.setColor(Color.BLACK);
            g.drawLine(MARGIN_LEFT, rulerHeight, MARGIN_RIGHT, rulerHeight);
            int second = 0;
            for (int i = 0; i * fps < values.length; i++) {
                int xi = nrToX(i * fps, values.length, dim.width);
                g.drawLine(xi, MARGIN_TOP - 5, xi, rulerHeight + 5);
                if (i % 5 == 0)
                    g.drawString(Integer.toString(second), xi - 5, rulerHeight + 15);
                second++;
            }
        }
    }

    private void drawLine(Graphics g, int i, int n, short value1, short value2, Dimension dim) {
        int x1 = nrToX(i, n, dim.width);
        int y1 = valueToY(value1, dim.height);
        int x2 = nrToX(i + 1, n, dim.width);
        int y2 = valueToY(value2, dim.height);
        //System.out.println("x1 = " + x1 + "; y1 = " + y1 + "; x2 = " + x2 + "; y2 = " + y2);
        g.drawLine(x1, y1, x2, y2);
    }

    private int nrToX(int i, int n, int width) {
        return i * (width - MARGIN_LEFT - MARGIN_RIGHT) / n + MARGIN_LEFT;
    }

    private int valueToY(short value, int height) {
        return ((int) -value - Short.MIN_VALUE) * (height - MARGIN_TOP - MARGIN_BOTTOM) / (1 << 16) + MARGIN_TOP;
    }

    public void setValues(short[] values) {
        this.values = values.clone();
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

    public static void displayViewer(final short[] values) {
        final SoundViewer viewer = new SoundViewer();
        viewer.setValues(values);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Sound Sample Viewer");
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
