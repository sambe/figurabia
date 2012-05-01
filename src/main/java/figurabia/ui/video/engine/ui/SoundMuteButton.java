/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 23.10.2011
 */
package figurabia.ui.video.engine.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

import figurabia.ui.video.engine.actorframework.MessageSendable;
import figurabia.ui.video.engine.messages.StatusRequest;

@SuppressWarnings("serial")
public class SoundMuteButton extends JComponent {

    private static final int SIZE = 20;
    private static final int HALF = SIZE / 2;
    private static final int QUARTER = SIZE / 4;
    private static final int FIFTH = SIZE / 5;

    // TODO actually this should not be static, but there should be a listener to update such fields
    private static boolean mute = false;

    private MessageSendable controller;

    public SoundMuteButton(MessageSendable controller) {
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
                    muteOrUnmute();
                }
            }
        });
    }

    public boolean isMute() {
        return mute;
    }

    public void setMute(boolean mute) {
        if (SoundMuteButton.mute != mute) {
            SoundMuteButton.mute = mute;
            repaint();
        }
    }

    private void muteOrUnmute() {
        // invert current mute state
        mute = !mute;
        controller.send(new StatusRequest(mute, null));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Color.DARK_GRAY);
        g.fillRect(0, 0, SIZE, SIZE);
        paintMuteButton(0, 0, (Graphics2D) g, mute);
    }

    private void paintMuteButton(int x, int y, Graphics2D g, boolean mute) {
        int[] xa = { x + QUARTER, x + HALF, x + SIZE - QUARTER, x + SIZE - QUARTER, x + HALF, x + QUARTER };
        int[] ya = { y + 2 * FIFTH, y + 2 * FIFTH, y + FIFTH, y + SIZE - FIFTH, y + SIZE - 2 * FIFTH,
                y + SIZE - 2 * FIFTH };

        g.setColor(Color.LIGHT_GRAY);
        g.fillPolygon(xa, ya, 6);

        if (mute) {
            g.setStroke(new BasicStroke(2));
            //g.drawLine(x + FIFTH, y + FIFTH, x + SIZE - FIFTH, y + SIZE - FIFTH);
            g.drawLine(x + SIZE - FIFTH, y + FIFTH, x + FIFTH, y + SIZE - FIFTH);
        }
    }
}
