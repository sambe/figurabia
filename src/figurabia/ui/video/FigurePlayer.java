/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 19.10.2009
 */
package figurabia.ui.video;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.format.VideoFormat;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import figurabia.domain.Figure;
import figurabia.framework.FigureModel;
import figurabia.framework.Workspace;
import figurabia.ui.framework.PlayerListener;
import figurabia.ui.video.engine.MediaPlayer;
import figurabia.ui.video.engine.PositionListener;
import figurabia.ui.video.engine.messages.CachedFrame;
import figurabia.ui.video.engine.messages.PositionUpdate;
import figurabia.ui.video.engine.ui.ControlBar;
import figurabia.ui.video.engine.ui.VideoScreen;

@SuppressWarnings("serial")
public class FigurePlayer extends JPanel {

    //private Workspace workspace;
    private FigureModel figureModel;

    private MediaPlayer mediaPlayer;
    private VideoScreen videoScreen;
    private ControlBar controlBar;

    private List<PlayerListener> playerListeners = new ArrayList<PlayerListener>();

    private int currentlyActivePosition = -1;

    private boolean inSetter = false;

    private boolean active = false;

    private long currentTime = 0;

    public FigurePlayer(Workspace workspace, MediaPlayer player, FigureModel figureModel_) {
        //this.workspace = workspace;
        this.figureModel = figureModel_;

        mediaPlayer = player;
        videoScreen = player.createScreen();
        controlBar = player.createControlBar();

        mediaPlayer.addPositionListener(new PositionListener() {
            private boolean previouslyActive = false;

            @Override
            public void receive(PositionUpdate update) {
                Figure figure = figureModel.getCurrentFigure();
                //System.out.println("DEBUG: FigurePlayer " + this + " figure = " + figure);
                if (figure == null || figure.getPositions().size() == 0)
                    return;
                if (active) {
                    currentTime = update.position * 1000000L;
                    List<Long> videoPos = figure.getVideoPositions();
                    // find the newest position
                    int newActive = findNearest(videoPos, currentTime);
                    // only send notifier update if the currently active really changed or the view was just activated
                    if (currentlyActivePosition != newActive || !previouslyActive) {
                        previouslyActive = true;
                        //System.out.println("DEBUG: new position: " + newActive + " (time = " + time + ")");
                        currentlyActivePosition = newActive;
                        notifyPlayerListeners(figure, currentlyActivePosition);
                    }
                } else {
                    if (previouslyActive == true) {
                        System.out.println("DEBUG: Switching previously active to false " + FigurePlayer.this);
                    }
                    previouslyActive = false;
                }
            }
        });

        setBackground(Color.BLACK);
        setOpaque(true);

        setLayout(new MigLayout("ins 0,gap 0", "[fill]", "[fill][22]"));
        add(videoScreen, "push, wrap");
        add(controlBar);
    }

    private int findNearest(List<Long> values, long location) {
        // cannot use java.util.Collections.binarySearch here because it returns -1 if the key is not found (but we need the next lower one)
        int l = 0, h = values.size() - 1;
        if (h < 0) {
            return -1;
        }
        while (l < h) {
            int m = (l + h + 1) / 2;
            if (values.get(m) < location) {
                l = m;
            } else {
                h = m - 1;
            }
        }
        if (values.get(l) < location) {
            h = l + 1;
        } else {
            l = l - 1;
        }
        if (l < 0)
            return h;
        if (h >= values.size())
            return l;

        return location - values.get(l) < values.get(h) - location ? l : h;
    }

    @Override
    protected void paintComponent(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    public void setPosition(int i) {
        boolean animated = currentlyActivePosition != -1 && Math.abs(i - currentlyActivePosition) == 1;
        setPosition(i, animated);
    }

    public void setPosition(int i, boolean animated) {
        if (inSetter) // prevent recursive setting of the position (would result in multiple times repositioning the video)
            return;

        // set media player to the correct position
        Figure figure = figureModel.getCurrentFigure();
        long videoTime = figure.getVideoPositions().get(i);
        if (animated)
            setVideoTimeAnimated(videoTime);
        else
            setVideoTime(videoTime);
        currentlyActivePosition = i;

        // explicit call still necessary because the normal timer will only notify the next change
        notifyPlayerListeners(figure, i);
    }

    public int getPosition() {
        return currentlyActivePosition;
    }

    public void addPlayerListener(PlayerListener l) {
        playerListeners.add(l);
    }

    public void removePlayerListener(PlayerListener l) {
        playerListeners.remove(l);
    }

    protected void notifyPlayerListeners(Figure f, int position) {
        inSetter = true;
        for (PlayerListener l : playerListeners) {
            try {
                l.positionActive(f, position);
            } catch (RuntimeException e) {
                // catch exceptions here to avoid unnecessary effects
                System.err.println("Exception from a PlayerListener. Figure: " + f + " position: " + position);
                e.printStackTrace();
            }
        }
        inSetter = false;
    }

    /**
     * Sets the player to the given position. If the player was running it will be started again after repositioning.
     * 
     * @param nanos the position in time to where the video should be set
     */
    public void setVideoTime(long nanos) {
        //System.out.println("DEBUG: explicitly setting the video time to " + nanos);
        mediaPlayer.setPosition(nanos / 1000000L);
    }

    private void setVideoTimeAnimated(long nanos) {
        mediaPlayer.setPositionAnimated(nanos / 1000000L);
    }

    public void setRepeatFigureOnly(boolean figureOnly) {
        if (figureOnly) {
            Figure f = figureModel.getCurrentFigure();
            List<Long> videoPositions = f.getVideoPositions();
            long first = videoPositions.get(0) / 1000000L;
            long last = videoPositions.get(videoPositions.size() - 1) / 1000000L;
            mediaPlayer.restrictPositionRange(first, last);
        } else {
            mediaPlayer.restrictPositionRange(null, null);
        }
    }

    /**
     * @return the activePlayer
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param active the activePlayer to set
     */
    public void setActive(boolean active) {
        this.active = active;
        if (active)
            activate();
    }

    private void activate() {
        mediaPlayer.setActiveScreen(videoScreen);
    }

    public long getVideoNanoseconds() {
        return currentTime;
    }

    public void captureCurrentImage(File pictureDir, int figureId, int bar, int beat, long videoNanoseconds) {
        String pictureName = String.format("%03d-%d.jpg", bar, beat);
        captureImage(videoNanoseconds, new File(pictureDir.getAbsolutePath() + File.separator + figureId
                + File.separator + pictureName));
    }

    private void captureImage(long videoNanoseconds, File targetFile) {
        long mediaTime = videoNanoseconds / 1000000L;
        VideoFormat format = mediaPlayer.getVideoFormat();
        double frameRate = format.getFrameRate();
        long frameSeqNum = (long) Math.floor(mediaTime * frameRate / 1000);
        CachedFrame frame = mediaPlayer.getFrame(frameSeqNum);
        if (frame.frame.isEndOfMedia()) {
            System.out.println("ERROR: Capturing frame: end of media.");
            return;
        }
        Image img = frame.frame.video.getImage();

        // code for creating the image and storing it to disk
        // maybe for this purpose: do not even save them, just reference
        try {
            BufferedImage outImage = new BufferedImage(format.getSize().width, format.getSize().height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics og = outImage.getGraphics();
            og.drawImage(img, 0, 0, format.getSize().width, format.getSize().height, null);
            // prepareImage(outImage,rheight,rheight, null);
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = writers.next();

            // Once an ImageWriter has been obtained, its destination must be set to an ImageOutputStream:
            ImageOutputStream ios = ImageIO.createImageOutputStream(targetFile);
            writer.setOutput(ios);

            // Finally, the image may be written to the output stream:
            writer.write(outImage);
            ios.close();
        } catch (IOException e) {
            System.out.println("ERROR: An IO problem occured during saving of the picture: "
                    + targetFile);
            e.printStackTrace();
        } finally {
            frame.recycle();
        }
    }
}
