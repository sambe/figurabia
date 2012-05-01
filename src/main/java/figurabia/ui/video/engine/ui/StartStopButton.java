/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 13.08.2011
 */
package figurabia.ui.video.engine.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import figurabia.ui.video.engine.actorframework.MessageSendable;
import figurabia.ui.video.engine.messages.ControlCommand;
import figurabia.ui.video.engine.messages.ControlCommand.Command;

public class StartStopButton extends JComponent {

    private static final int SIZE = 20;
    private static final int QUARTER = SIZE / 4;
    private static final int FIFTH = SIZE / 5;

    private boolean running = false;

    private MessageSendable controller;

    public StartStopButton(MessageSendable controller) {
        this.controller = controller;
        setOpaque(true);
        Dimension size = new Dimension(SIZE, SIZE);
        setMinimumSize(size);
        setPreferredSize(size);
        setMaximumSize(size);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    startOrStop();
                }
            }
        });
    }

    private void startOrStop() {
        if (running) {
            controller.send(new ControlCommand(Command.STOP));
        } else {
            controller.send(new ControlCommand(Command.START));
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        if (this.running != running) {
            this.running = running;
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, SIZE, SIZE);
        if (running) {
            paintStopButton(0, 0, g);
        } else {
            paintPlayButton(0, 0, g);
        }
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
}
