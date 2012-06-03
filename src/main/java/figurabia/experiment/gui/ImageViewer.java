/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on May 7, 2012
 */
package figurabia.experiment.gui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class ImageViewer extends JPanel {

    private Image[] images;
    private int active = 0;

    public ImageViewer() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == 1) {
                    active = (active + 1) % images.length;
                    repaint();
                } else if (e.getButton() == 3) {
                    active = (active + images.length - 1) % images.length;
                    repaint();
                }
            }
        });
    }

    public void setImages(Image... images) {
        this.images = images;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (images != null && images.length != 0) {
            Image img = images[active];
            g.drawImage(img, 0, 0, null);
        }
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

    public static void displayViewer(Image... images) {
        ImageViewer viewer = new ImageViewer();
        viewer.setImages(images);
        displayViewer(viewer);
        viewer.waitForContinue();
    }

    public static void displayViewerAsync(Image... images) {
        ImageViewer viewer = new ImageViewer();
        viewer.setImages(images);
        displayViewer(viewer);
    }

    private static void displayViewer(final ImageViewer viewer) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("Image Viewer");
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
    }
}
