/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 19.10.2009
 */
package figurabia.ui.video;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import exmoplay.access.VideoFormat;
import exmoplay.engine.MediaPlayer;
import exmoplay.engine.PositionListener;
import exmoplay.engine.messages.CachedFrame;
import exmoplay.engine.messages.PositionUpdate;
import exmoplay.engine.ui.ControlBar;
import exmoplay.engine.ui.VideoScreen;
import figurabia.domain.Figure;
import figurabia.framework.FigurabiaModel;
import figurabia.io.BeatPictureCache;
import figurabia.ui.framework.PlayerListener;

@SuppressWarnings("serial")
public class FigurePlayer extends JPanel {

    private final BeatPictureCache beatPictureCache;
    private final FigurabiaModel figurabiaModel;

    private MediaPlayer mediaPlayer;
    private VideoScreen videoScreen;
    private ControlBar controlBar;

    private List<PlayerListener> playerListeners = new ArrayList<PlayerListener>();

    private int currentlyActiveIndex = -1;

    private boolean inSetter = false;

    private boolean active = false;
    private boolean previouslyActive = false;
    private long lastUpdatedPosition = 0;

    private long currentTime = 0;

    public FigurePlayer(BeatPictureCache bpc, MediaPlayer player, FigurabiaModel fm) {
        this.beatPictureCache = bpc;
        this.figurabiaModel = fm;

        mediaPlayer = player;
        videoScreen = player.createScreen();
        controlBar = player.createControlBar();

        mediaPlayer.addPositionListener(new PositionListener() {
            @Override
            public void receive(PositionUpdate update) {
                onPositionUpdate(update.position);
            }
        });

        //setBackground(Color.BLACK);
        setOpaque(true);

        setLayout(new MigLayout("ins 0,gap 0", "[fill]", "[fill][22]"));
        add(videoScreen, "push, wrap");
        add(controlBar);
    }

    private void onPositionUpdate(long position) {
        lastUpdatedPosition = position;
        Figure figure = figurabiaModel.getCurrentFigure();
        //System.out.println("DEBUG: FigurePlayer " + this + " figure = " + figure);
        if (figure == null || figure.getPositions().size() == 0)
            return;
        if (active) {
            currentTime = position * 1000000L;
            List<Long> videoPos = figure.getVideoPositions();
            // find the newest position
            int newActive = findNearest(videoPos, currentTime);
            // only send notifier update if the currently active really changed or the view was just activated
            if (currentlyActiveIndex != newActive || !previouslyActive) {
                previouslyActive = true;
                //System.out.println("DEBUG: new position: " + newActive + " (time = " + time + ")");
                currentlyActiveIndex = newActive;
                notifyPlayerListeners(figure, currentlyActiveIndex);
            }
        } else {
            if (previouslyActive == true) {
                System.out.println("DEBUG: Switching previously active to false " + FigurePlayer.this);
            }
            previouslyActive = false;
        }
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

    public void setCurrentIndex(int i) {
        boolean animated = currentlyActiveIndex != -1 && Math.abs(i - currentlyActiveIndex) == 1;
        setCurrentIndex(i, animated);
    }

    public void setCurrentIndex(int i, boolean animated) {
        if (inSetter) // prevent recursive setting of the position (would result in multiple times repositioning the video)
            return;

        // set media player to the correct position
        Figure figure = figurabiaModel.getCurrentFigure();
        long videoTime = figure.getVideoPositions().get(i);
        if (animated)
            setVideoTimeAnimated(videoTime);
        else
            setVideoTime(videoTime);
        currentlyActiveIndex = i;

        // explicit call still necessary because the normal timer will only notify the next change
        notifyPlayerListeners(figure, i);
    }

    public int getCurrentIndex() {
        return currentlyActiveIndex;
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
                l.update(f, position);
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
        Figure f = figurabiaModel.getCurrentFigure();
        if (f == null)
            return;
        if (figureOnly) {
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
        onPositionUpdate(lastUpdatedPosition);
    }

    public long getVideoNanoseconds() {
        return currentTime;
    }

    public void captureCurrentImage(String figureId, int bar, int beat, long videoNanoseconds) {
        long mediaTime = videoNanoseconds / 1000000L;
        VideoFormat format = mediaPlayer.getVideoFormat();
        double frameRate = format.getFrameRate();
        long frameSeqNum = (long) Math.floor(mediaTime * frameRate / 1000);
        CachedFrame frame = mediaPlayer.getFrame(frameSeqNum);
        try {
            if (frame.frame.isEndOfMedia()) {
                System.out.println("ERROR: Capturing frame: end of media.");
                return;
            }
            Image image = frame.frame.video.getImage();

            beatPictureCache.storePicture(figureId, bar, beat, image);
        } finally {
            frame.recycle();
        }
    }
}
