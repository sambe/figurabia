/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 13.06.2011
 */
package figurabia.ui.video.engine.ui;

import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import figurabia.ui.util.SimplePanelFrame;
import figurabia.ui.video.engine.actorframework.MessageSendable;
import figurabia.ui.video.engine.actorframework.RegisterForUpdates;
import figurabia.ui.video.engine.messages.PositionUpdate;
import figurabia.ui.video.engine.messages.StateUpdate;

@SuppressWarnings("serial")
public class ControlBar extends JPanel {

    private static final int SIZE = 20;
    private static final int QUARTER = SIZE / 4;
    private static final int FIFTH = SIZE / 5;

    private static final int MIN_MILLIS_BETWEEN_REQUESTS = 50;

    private StartStopButton startStopButton;
    private SpeedControl speedControl;
    private MovieProgressBar movieProgressBar;

    private MessageSendable controller;

    private Rectangle buttonBounds = new Rectangle(0, 0, SIZE, SIZE);

    public ControlBar(MessageSendable controller) {
        this.controller = controller;

        startStopButton = new StartStopButton(controller);
        speedControl = new SpeedControl(controller);
        movieProgressBar = new MovieProgressBar(controller);

        setLayout(new MigLayout("ins 0,gap 0", "[fill][fill][fill]", "[fill]"));
        add(startStopButton, "");
        add(speedControl, "");
        add(movieProgressBar, "push");

        controller.send(new RegisterForUpdates(PositionUpdate.class, new MessageSendable() {
            @Override
            public void send(Object message) {
                final PositionUpdate pu = (PositionUpdate) message;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        movieProgressBar.updatePosition(pu.position, pu.startPosition, pu.endPosition);
                    }
                });
            }
        }));
        controller.send(new RegisterForUpdates(StateUpdate.class, new MessageSendable() {
            @Override
            public void send(Object message) {
                final StateUpdate su = (StateUpdate) message;
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        switch (su.state) {
                        case PREPARING:
                        case PLAYING:
                            startStopButton.setRunning(true);
                            break;
                        case STOPPED:
                            startStopButton.setRunning(false);
                            break;
                        default:
                            throw new IllegalStateException("unsupported state: " + su.state);
                        }
                    }
                });
            }
        }));
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
