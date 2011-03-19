/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 19.10.2009
 */
package figurabia.ui.video;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.media.Buffer;
import javax.media.Controller;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.RealizeCompleteEvent;
import javax.media.StartEvent;
import javax.media.StopEvent;
import javax.media.Time;
import javax.media.bean.playerbean.MediaPlayer;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import com.omnividea.media.renderer.video.Java2DRenderer;

import figurabia.domain.Figure;
import figurabia.framework.FigureModel;
import figurabia.framework.Workspace;
import figurabia.ui.framework.PlayerListener;

@SuppressWarnings("serial")
public class FigurePlayer extends JPanel {

    //private Workspace workspace;
    private FigureModel figureModel;

    private MediaPlayer mediaPlayer;
    private Component playerControl;
    private VideoScreen videoScreen;

    private int positionToSwitchToAfterStart = -1;

    private List<PlayerListener> playerListeners = new ArrayList<PlayerListener>();

    private int currentlyActivePosition = -1;

    private boolean repeatFigureOnly = false;

    private boolean inSetter = false;

    private boolean active = false;

    public FigurePlayer(Workspace workspace, MediaPlayer player, FigureModel figureModel_) {
        //this.workspace = workspace;
        this.figureModel = figureModel_;

        mediaPlayer = player;
        mediaPlayer.setFixedAspectRatio(true); //TODO move this to where the player is created
        videoScreen = new VideoScreen();

        // add polling to repeatedly find out whether the position has changed
        Timer playerProgressPollingTimer = new Timer(50, new ActionListener() {
            private boolean previouslyActive = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                Figure figure = figureModel.getCurrentFigure();
                //System.out.println("DEBUG: FigurePlayer " + this + " figure = " + figure);
                if (figure == null || figure.getPositions().size() == 0)
                    return;
                if (active) {
                    long currentTime = mediaPlayer.getMediaNanoseconds();
                    List<Long> videoPos = figure.getVideoPositions();
                    // find the newest position
                    /*int newActive = -1;
                    for (int i = 0; i < videoPos.size(); i++) {
                        if (videoPos.get(i) > currentTime) {
                            newActive = i - 1;
                            break;
                        }
                    }
                    // if player is past the last position
                    if (newActive == -1 && videoPos.get(videoPos.size() - 1) <= currentTime) {
                        newActive = videoPos.size() - 1;
                    }*/
                    int newActive = findNearest(videoPos, currentTime);
                    // only send notifier update if the currently active really changed or the view was just activated
                    if (currentlyActivePosition != newActive || !previouslyActive) {
                        previouslyActive = true;
                        //System.out.println("DEBUG: new position: " + newActive + " (time = " + time + ")");
                        currentlyActivePosition = newActive;
                        notifyPlayerListeners(figure, currentlyActivePosition);
                    }

                    if (repeatFigureOnly && currentTime > videoPos.get(videoPos.size() - 1)) {
                        setPosition(0);
                    }
                } else {
                    if (previouslyActive == true) {
                        System.out.println("DEBUG: Switching previously active to false " + FigurePlayer.this);
                    }
                    previouslyActive = false;
                }
            }
        });
        playerProgressPollingTimer.start();

        mediaPlayer.addControllerListener(new ControllerListener() {
            @Override
            public void controllerUpdate(ControllerEvent event) {
                if (event instanceof StartEvent) {
                    videoScreen.setRunning(true);
                }
                else if (event instanceof StopEvent) {
                    videoScreen.setRunning(false);
                }

                if (!active)
                    return;
                //System.out.println("DEBUG: state = " + event.getSourceController().getState()
                //        + "  positionToSwitchTo = " + positionToSwitchToAfterStart);
                if (event instanceof RealizeCompleteEvent && positionToSwitchToAfterStart != -1) {
                    System.out.println("DEBUG: changing position now to " + positionToSwitchToAfterStart);
                    final int newPosition = positionToSwitchToAfterStart;
                    positionToSwitchToAfterStart = -1;
                    // invoked asynchronously, because I'm unsure if this is the Swing Dispatch Thread
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            activate();

                            if (newPosition < figureModel.getCurrentFigure().getPositions().size()) {
                                setPosition(newPosition);
                            }
                        }
                    });
                }

            }
        });

        setBackground(Color.BLACK);
        setOpaque(true);

        setLayout(new MigLayout("ins 0,gap 0", "[fill]", "[fill][22]"));
        add(videoScreen, "push, wrap");
    }

    private int findNearest(List<Long> values, long location) {
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

    public void setPositionWhenReady(int i) {
        // cannot change the position right here because otherwise the player will
        // not be prefetched yet and will throw an exception
        if (i != -1) {
            positionToSwitchToAfterStart = i;
        }
    }

    public void setPosition(int i) {
        if (inSetter) // prevent recursive setting of the position (would result in multiple times repositioning the video)
            return;

        // set media player to the correct position
        Figure figure = figureModel.getCurrentFigure();
        setVideoTime(figure.getVideoPositions().get(i));
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

        int state = mediaPlayer.getPlayer().getState();
        if (state == Controller.Unrealized || state == Controller.Realizing) {
            System.err.println("ERROR: setVideoTime: player is in unrealized state (" + state + ").");
            return;
        }

        // first stop the
        boolean wasRunning = false;
        if (mediaPlayer.getPlayer().getState() == Controller.Started) {
            //System.out.println("DEBUG: explicitly stopping now...");
            mediaPlayer.stop();
            wasRunning = true;
        }

        // TODO currently this is not working reliably, because of interference
        //      between list selection and video positioning
        /*// if time to set is less than 2 seconds ahead, let the player run to it
        if (!wasRunning) {
            long currentNanos = mediaPlayer.getMediaNanoseconds();
            if (currentNanos < nanos && nanos - currentNanos < 3000000000L) {
                System.out.println("DEBUG: trying to make the video play to the position");
                //mediaPlayer.getPlayer().setStopTime(Player.RESET);
                mediaPlayer.getPlayer().setStopTime(new Time(nanos));
                mediaPlayer.start();
                return;
            }
        }*/

        mediaPlayer.setMediaTime(new Time(nanos));
        if (wasRunning) {
            //System.out.println("DEBUG: explicitly restarting now...");
            mediaPlayer.start();
        }
    }

    public void setRepeatFigureOnly(boolean figureOnly) {
        repeatFigureOnly = figureOnly;
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
        Java2DRenderer renderer = Java2DRenderer.getNewestInstance();
        //SwingRenderer renderer = SwingRenderer.getNewestInstance();
        if (renderer == null)
            return;
        videoScreen.setJava2DRenderer(renderer);
        //videoScreen.setSwingRenderer(renderer);
        if (playerControl != null) {
            remove(playerControl);
        }
        playerControl = mediaPlayer.getPlayer().getControlPanelComponent();
        add(playerControl);
        validate();
    }

    public long getVideoNanoseconds() {
        return mediaPlayer.getMediaNanoseconds();
    }

    public void setPositionAndCaptureImage(long time, File pictureDir, int figureId, int bar, int beat) {
        mediaPlayer.setMediaTime(new Time(time));
        mediaPlayer.prefetch();
        mediaPlayer.waitForState(Controller.Prefetched);

        captureCurrentImage(pictureDir, figureId, bar, beat);
    }

    public void captureCurrentImage(File pictureDir, int figureId, int bar, int beat) {
        String pictureName = String.format("%03d-%d.jpg", bar, beat);
        captureImage(new File(pictureDir.getAbsolutePath() + File.separator + figureId + File.separator + pictureName));
    }

    private void captureImage(File targetFile) {
        Buffer frame = videoScreen.getFrameGrabbingControl().grabFrame();
        if (frame == null) {
            System.out.println("ERROR: Capturing frame: no frame received.");
            return;
        }
        VideoFormat format = (VideoFormat) frame.getFormat();
        BufferToImage b2i = new BufferToImage(format);
        Image img = b2i.createImage(frame);

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
                    + frame.getSequenceNumber() + ".jpg");
            e.printStackTrace();
        }
    }
}
