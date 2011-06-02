/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 10.04.2011
 */
package figurabia.ui.video.engine;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import figurabia.ui.video.engine.actorframework.Actor;
import figurabia.ui.video.engine.messages.CachedFrame;
import figurabia.ui.video.engine.messages.CurrentScreen;
import figurabia.ui.video.engine.messages.Repaint;

public class VideoRenderer extends Actor {

    private VideoScreen currentScreen;

    private CachedFrame currentFrame;

    private Image currentImage;

    public VideoRenderer(Actor errorHandler) {
        super(errorHandler);
    }

    @Override
    protected void act(Object message) {
        if (message instanceof CachedFrame) {
            if (currentFrame != null) {
                currentFrame.recycle();
                currentFrame = null;
            }
            currentFrame = (CachedFrame) message;
            currentImage = currentFrame.frame.video.getImage();
            paintImageOnScreen(currentScreen, currentImage);
        } else if (message instanceof CurrentScreen) {
            currentScreen = ((CurrentScreen) message).screen;
        } else if (message instanceof Repaint) {
            Repaint repaint = (Repaint) message;
            paintImageOnScreen(repaint.screen, currentImage);
        } else {
            throw new IllegalArgumentException("unknown type of message: " + message.getClass().getName());
        }

    }

    /*@Override
    protected void idle() {
        super.idle();

        if (timer.isRunning() && currentScreen != null && !cachedFrames.isEmpty()) {
            // paint current frame if it changed
            long currentSeqNum = (long) Math.floor(timer.getPosition() / videoFormat.getFrameRate());
            if (cachedFrames.peek().seqNum != currentSeqNum) {
                // remove frame and recycle
                recycler.send(new RecyclingBag(cachedFrames.poll()));
                currentImage = null; // reset image because it might be based on the buffer that is recycled

                if (!cachedFrames.isEmpty()) {
                    // paint image on screen
                    currentImage = cachedFrames.peek().frame.video.getImage();
                    paintImageOnScreen(currentScreen, currentImage);
                }
            }

        }
    }*/

    private void paintImageOnScreen(VideoScreen screen, Image image) {
        Graphics2D g2d = (Graphics2D) screen.getGraphics();

        if (g2d != null) {
            if (image == null) {
                paintBlackRectangle(screen, g2d);
            } else {
                paintImage(currentScreen, g2d, image);
            }
        }
    }

    private void paintBlackRectangle(VideoScreen screen, Graphics g) {
        int x = 0;
        int y = 0;
        int width = screen.getWidth();
        int height = screen.getHeight();

        g.setColor(Color.BLACK);
        g.fillRect(x, y, width, height);
    }

    private void paintImage(VideoScreen screen, Graphics g, Image image) {
        int x = 0;
        int y = 0;
        int width = screen.getWidth();
        int height = screen.getHeight();

        g.setColor(Color.BLACK);
        if (width * 3 > height * 4) { // if wider than 4:3
            int d = (width - height * 4 / 3);
            x += d / 2;
            width -= d;
            g.fillRect(0, 0, d / 2, height);
            g.fillRect(width + d / 2, 0, (d + 1) / 2, height);
        } else { // if higher than 4:3
            int d = (height - width * 3 / 4);
            y += d / 2;
            height -= d;
            g.fillRect(0, 0, width, d / 2);
            g.fillRect(0, height + d / 2, width, (d + 1) / 2);
        }

        g.drawImage(image, x, y, width, height, null);
    }

}
