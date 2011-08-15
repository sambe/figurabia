/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 13.08.2011
 */
package figurabia.ui.video.engine.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import figurabia.ui.video.engine.actorframework.MessageSendable;
import figurabia.ui.video.engine.messages.SetPosition;

public class MovieProgressBar extends JComponent {

    private static final int SIZE = 20;
    private static final int QUARTER = SIZE / 4;
    private static final int FIFTH = SIZE / 5;

    private static final int MIN_MILLIS_BETWEEN_REQUESTS = 50;

    private double barMinValue = 0.0;
    private double barMaxValue = 0.0;
    private double barPositionValue = 0.0;

    private boolean draggingBarPosition = false;
    private long lastDragMessageSent = 0;

    private MessageSendable controller;

    public MovieProgressBar(MessageSendable controller) {
        this.controller = controller;
        setOpaque(true);

        MouseAdapter mouseAdapter = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                // TODO detect if cursor is on position mark (capture mouse)
                draggingBarPosition = true;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // TODO move position, if cursor was on position mark
                if (draggingBarPosition) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime > lastDragMessageSent + MIN_MILLIS_BETWEEN_REQUESTS) {
                        lastDragMessageSent = currentTime;
                        movePosition(e.getX());
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingBarPosition) {
                    draggingBarPosition = false;
                    movePosition(e.getX());
                }
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    private void movePosition(int x) {
        // TODO need to know the whole duration of the media and set it as maximum
        int screenPosition = x - QUARTER;
        int barLength = getWidth() - 2 * QUARTER;
        if (screenPosition < 0)
            screenPosition = 0;
        if (screenPosition > barLength)
            screenPosition = barLength;
        long moviePosition = (long) (barMinValue + (barMaxValue - barMinValue) * screenPosition / barLength);
        controller.send(new SetPosition(moviePosition));
        barPositionValue = moviePosition;
        repaint();
    }

    public void updatePosition(double pos, double min, double max) {
        if (!draggingBarPosition) {
            barMinValue = min;
            barMaxValue = max;
            barPositionValue = pos;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, width, SIZE);
        paintProgressBar(0, 0, width, g);
    }

    private void paintProgressBar(int x, int y, int w, Graphics g) {
        int x1 = x + QUARTER;
        int y1 = y + QUARTER;
        int w1 = w - 2 * QUARTER;
        int h1 = SIZE - 2 * QUARTER;

        g.setColor(Color.GRAY);
        g.fillRect(x1, y1, w1, h1);

        int xp = (int) Math.round(w1 * (barPositionValue - barMinValue) / (barMaxValue - barMinValue));
        int x2 = x1 + xp - FIFTH / 2;
        int y2 = y + FIFTH;
        int w2 = FIFTH;
        int h2 = SIZE - 2 * FIFTH;

        g.setColor(Color.BLACK);
        g.fillRect(x2, y2, w2, h2);
    }
}
