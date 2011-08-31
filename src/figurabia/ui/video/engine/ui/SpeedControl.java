/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 13.08.2011
 */
package figurabia.ui.video.engine.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import figurabia.ui.video.engine.actorframework.MessageSendable;
import figurabia.ui.video.engine.messages.SetSpeed;

public class SpeedControl extends JComponent {

    private static final int SIZE = 20;
    private static final int QUARTER = SIZE / 4;
    private static final int FIFTH = SIZE / 5;

    private static final double[] SPEEDS = { -4.0, -3.0, -2.0, -1.5, -1.0, -0.75, -0.5, -1.0 / 3.0, -0.25, 0.25,
            1.0 / 3.0, 0.5, 0.75, 1.0, 1.5, 2.0, 3.0, 4.0 };

    private MessageSendable controller;

    private static int speedIndex = 13;
    private double currentSpeed = SPEEDS[speedIndex];

    public SpeedControl(MessageSendable controller) {
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
                    speedIndex = (speedIndex + 1) % SPEEDS.length;
                    setSpeed(SPEEDS[speedIndex]);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    speedIndex = (speedIndex + SPEEDS.length - 1) % SPEEDS.length;
                    setSpeed(SPEEDS[speedIndex]);
                }
            }
        });
    }

    private void setSpeed(double speed) {
        if (speed != currentSpeed) {
            currentSpeed = speed;
            controller.send(new SetSpeed(speed));
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, SIZE, SIZE);

        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font("Dialog", Font.PLAIN, 10));
        g.drawString(Double.toString(currentSpeed), 0, SIZE - SIZE / 3);
    }
}
