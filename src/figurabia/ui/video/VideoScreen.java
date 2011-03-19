/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 14.02.2010
 */
package figurabia.ui.video;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.media.control.FrameGrabbingControl;
import javax.swing.JComponent;

import com.omnividea.media.renderer.video.Java2DRenderer;

@SuppressWarnings("serial")
public class VideoScreen extends JComponent {

    //private SwingRenderer renderer;
    private Java2DRenderer renderer;

    private boolean active;

    private boolean running;

    public VideoScreen() {
        setBackground(Color.BLACK);
        setIgnoreRepaint(true);
        setDoubleBuffered(true);
        setOpaque(true);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                System.out.println("DEBUG: " + VideoScreen.this + " on top now.");
                VideoScreen.this.renderer.setCurrentScreen(VideoScreen.this);
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (renderer != null)
            renderer.paintOnComponent(this, g);
        //renderer.paint(g);
    }

    /**
     * @return the connected
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param connected the connected to set
     */
    public void setActive(boolean connected) {
        this.active = connected;
    }

    public void setJava2DRenderer(Java2DRenderer renderer) {
        this.renderer = renderer;
        renderer.setCurrentScreen(this);
    }

    /*public void setSwingRenderer(SwingRenderer renderer) {
        this.renderer = renderer;
        renderer.setCurrentScreen(this);
    }*/

    public FrameGrabbingControl getFrameGrabbingControl() {
        return renderer;
    }

    /**
     * @return whether the player is running right now
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @param running
     */
    public void setRunning(boolean running) {
        this.running = running;
    }
}
