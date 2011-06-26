/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 13.06.2011
 */
package figurabia.ui.video.engine.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import figurabia.ui.util.SimplePanelFrame;
import figurabia.ui.video.engine.actorframework.MessageSendable;
import figurabia.ui.video.engine.messages.ControlCommand;
import figurabia.ui.video.engine.messages.SetPosition;
import figurabia.ui.video.engine.messages.ControlCommand.Command;

@SuppressWarnings("serial")
public class ControlBar extends JComponent {

    private static final int SIZE = 20;
    private static final int QUARTER = SIZE / 4;
    private static final int FIFTH = SIZE / 5;

    private static final int MIN_MILLIS_BETWEEN_REQUESTS = 50;

    private boolean running = false;
    private double barMinValue = 0.0;
    private double barMaxValue = 90000.0;
    private double barPositionValue = 0.0;

    private boolean draggingBarPosition = false;
    private long lastDragMessageSent = 0;

    private MessageSendable controller;

    private Rectangle buttonBounds = new Rectangle(0, 0, SIZE, SIZE);

    public ControlBar(MessageSendable controller) {
        this.controller = controller;

        MouseAdapter mouseAdapter = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                // detect button clicks (and clicks on free area of bar)
                if (buttonBounds.contains(e.getPoint())) {
                    startOrStop();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // TODO detect if cursor is on position mark (capture mouse)
                if (!buttonBounds.contains(e.getPoint())) {
                    draggingBarPosition = true;
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                // TODO move position, if cursor was on position mark
                if (draggingBarPosition) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime > lastDragMessageSent + MIN_MILLIS_BETWEEN_REQUESTS) {
                        lastDragMessageSent = currentTime;
                        movePosition(e.getX() - SIZE);
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingBarPosition) {
                    draggingBarPosition = false;
                    movePosition(e.getX() - SIZE);
                }
            }
        };
        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);

    }

    private void startOrStop() {
        if (running) {
            controller.send(new ControlCommand(Command.STOP));
            running = false;
        } else {
            controller.send(new ControlCommand(Command.START));
            running = true;
        }
        repaint();
    }

    private void movePosition(int x) {
        // TODO need to know the whole duration of the media and set it as maximum
        int screenPosition = x - QUARTER;
        int barLength = getWidth() - SIZE - 2 * QUARTER;
        if (screenPosition < 0)
            screenPosition = 0;
        if (screenPosition > barLength)
            screenPosition = barLength;
        long moviePosition = (long) (barMinValue + (barMaxValue - barMinValue) * screenPosition / barLength);
        controller.send(new SetPosition(moviePosition));
        barPositionValue = moviePosition;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        int width = getWidth();
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, width, SIZE);
        if (running) {
            paintStopButton(0, 0, g);
        } else {
            paintPlayButton(0, 0, g);
        }
        paintProgressBar(SIZE, 0, width - SIZE, g);
    }

    private void paintPlayButton(int x, int y, Graphics g) {
        int[] xa = { x + QUARTER, x + SIZE - QUARTER, x + QUARTER };
        int[] ya = { y + FIFTH, y + SIZE / 2, y + SIZE - FIFTH };

        g.setColor(Color.LIGHT_GRAY);
        g.fillPolygon(xa, ya, 3);
    }

    private void paintStopButton(int x, int y, Graphics g) {
        int h = SIZE - 2 * FIFTH;
        int w = QUARTER;
        int x1 = x + FIFTH;
        int y1 = y + FIFTH;
        int x2 = x + SIZE - FIFTH - QUARTER;
        int y2 = y1;

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(x1, y1, w, h);
        g.fillRect(x2, y2, w, h);
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

    public static void main(String[] args) {
        MessageSendable ms = new MessageSendable() {
            @Override
            public void send(Object message) {
                System.out.println("Sent message: " + message);
            }
        };
        new SimplePanelFrame(new ControlBar(ms), 400, SIZE + 40);
    }
}
