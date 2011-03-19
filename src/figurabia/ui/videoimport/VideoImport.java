/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 07.06.2009
 */
package figurabia.ui.videoimport;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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
import javax.media.Time;
import javax.media.bean.playerbean.MediaPlayer;
import javax.media.control.FrameGrabbingControl;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.io.FileUtils;

import figurabia.domain.Figure;
import figurabia.framework.PersistenceProvider;
import figurabia.framework.Workspace;
import figurabia.framework.simpleimpl.SimpleWorkspace;
import figurabia.math.LinearLeastSquares;
import figurabia.persistence.XStreamPersistenceProvider;
import figurabia.ui.util.SimplePanelFrame;

@SuppressWarnings("serial")
public class VideoImport extends JPanel {

    private MediaPlayer mediaPlayer;
    private FrameGrabbingControl grabber;
    private BeatBar beatBar;
    private VideoImportPanel buttonsPanel;
    private JPanel controlPanel;

    private JFrame frame;

    private PersistenceProvider persistenceProvider;
    private Workspace workspace;

    private Dimension prefSize;

    public VideoImport(String location, PersistenceProvider pp, Workspace w) {

        persistenceProvider = pp;
        workspace = w;

        buttonsPanel = new VideoImportPanel();
        beatBar = new BeatBar();
        beatBar.addSelectionChangedListener(new BeatBar.SelectionChangedListener() {
            @Override
            public void selectionChanged() {
                long time = beatBar.getSelectedBeatTime();
                if (time != -1) {
                    setVideoTime(time);
                }
            }
        });
        controlPanel = new JPanel();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setFixedAspectRatio(true);
        mediaPlayer.setMediaLocation(location);
        mediaPlayer.start();

        grabber = (FrameGrabbingControl) mediaPlayer.getControl("javax.media.control.FrameGrabbingControl");

        controlPanel.setLayout(new MigLayout());
        controlPanel.add(beatBar, "growx,wrap,pushx");
        controlPanel.add(buttonsPanel, "growx");

        setLayout(new MigLayout());
        add(mediaPlayer, "al center, wrap");
        add(controlPanel, "growx,pushx");

        // install a listener that will automatically resize
        prefSize = getPreferredSize();
        mediaPlayer.addControllerListener(new ControllerListener() {
            @Override
            public void controllerUpdate(ControllerEvent event) {
                // because we are not in the Swing Event Dispatch Thread
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Dimension newPrefSize = getPreferredSize();
                        if (!prefSize.equals(newPrefSize)) {
                            frame.setSize(new Dimension(newPrefSize.width + 45, newPrefSize.height + 45));
                            prefSize = newPrefSize;
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void validateTree() {
        // add handler that can detect, when the window was closed and correctly disposes the player (possibly stopping it first)
        // This is done here, because it needs to be done, after the tree has been assembled.
        Container c = this;
        while (c.getParent() != null) {
            c = c.getParent();
        }
        if (c instanceof JFrame) {
            frame = (JFrame) c;
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    mediaPlayer.stopAndDeallocate();
                }
            });
        }
        // continue normal tree validation
        super.validateTree();
    }

    public void setVideoLocation(String location) {
        mediaPlayer.setMediaLocation(location);
    }

    private class VideoImportPanel extends JPanel {

        private JButton oneButton;
        private JButton fiveButton;

        private JButton normalizeButton;
        private JButton importButton;

        public VideoImportPanel() {
            oneButton = new JButton(" 1 ");
            oneButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    beatBar.setVideoLength(mediaPlayer.getPlayer().getDuration().getNanoseconds());
                    beatBar.addBeatOn1(mediaPlayer.getMediaNanoseconds());
                }
            });
            fiveButton = new JButton(" 5 ");
            fiveButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    beatBar.setVideoLength(mediaPlayer.getPlayer().getDuration().getNanoseconds());
                    beatBar.addBeatOn5(mediaPlayer.getMediaNanoseconds());
                }
            });

            normalizeButton = new JButton("Normalize");
            normalizeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    normalize();
                }
            });

            importButton = new JButton("Import");
            importButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    importAsFigure();
                }
            });
            importButton.setDefaultCapable(true);

            setLayout(new MigLayout("", "push[][][]push[]", ""));
            add(oneButton);
            add(fiveButton);
            add(normalizeButton);
            add(importButton);
        }

    }

    private void normalize() {
        List<Long> normalizedBeats = LinearLeastSquares.fitToBars(new ArrayList<Long>(beatBar.getBeatsOn1()),
                new ArrayList<Long>(beatBar.getBeatsOn5()));
        List<Long> beatsOn1 = new ArrayList<Long>();
        List<Long> beatsOn5 = new ArrayList<Long>();
        for (int i = 0; i < normalizedBeats.size(); i++) {
            if (i % 2 == 0) {
                beatsOn1.add(normalizedBeats.get(i));
            } else {
                beatsOn5.add(normalizedBeats.get(i));
            }
        }
        beatBar.setBeatsOn1(beatsOn1);
        beatBar.setBeatsOn5(beatsOn5);
        beatBar.selectBeat(-1, -1);
    }

    private boolean isInputValid() {
        // check if there is any input at all
        if (beatBar.getBeatsOn1().size() == 0 && beatBar.getBeatsOn5().size() == 0) {
            JOptionPane.showMessageDialog(this, "Please enter some beats first.");
            return false;
        }

        // check if it starts with a 1 and ends with a 1 and has exactly one 5 inbetween every 1
        if (!areBeats1And5Alternate()) {
            JOptionPane.showMessageDialog(this, "For importing a video the beats must be set as follows:\n"
                    + " 1) the first and last beat is a 1\n" + " 2) between every 1 there is exactly one 5\n"
                    + "Please check that there are no double entries that are so close that they look like one.");
            return false;
        }

        return true;
    }

    private boolean areBeats1And5Alternate() {
        List<Long> beats1 = beatBar.getBeatsOn1();
        List<Long> beats5 = beatBar.getBeatsOn5();

        if (beats1.get(0) >= beats5.get(0) || beats1.get(beats1.size() - 1) <= beats5.get(beats5.size() - 1))
            return false;
        if (beats1.size() != beats5.size() + 1)
            return false;

        for (int i = 0; i < beats5.size(); i++) {
            if (beats1.get(i) >= beats5.get(i) || beats5.get(i) >= beats1.get(i + 1))
                return false;
        }

        return true;
    }

    public void importAsFigure() {

        // check whether the current input is okay
        if (!isInputValid())
            return;

        // stop the player if it should still be started
        if (mediaPlayer.getState() == Controller.Started) {
            mediaPlayer.stop();
        }

        // get the frame grabbing control, which allows to capture frames
        grabber = (FrameGrabbingControl) mediaPlayer.getControl("javax.media.control.FrameGrabbingControl");
        if (grabber == null) {
            System.out.println("DEBUG: no FrameGrabbingControl supported");
            return;
        }

        // start importing the video and screens
        try {
            // copy video into workspace
            File videoDir = workspace.getVideoDir();
            File originalVideoFile = new File(mediaPlayer.getMediaLocation().replace("file:", ""));
            String videoName = originalVideoFile.getName();
            File newVideoFile = new File(videoDir.getAbsolutePath() + File.separator + videoName);
            FileUtils.copyFile(originalVideoFile, newVideoFile);

            // create figure object and persist it
            Figure f = new Figure();
            f.setVideoName(videoName);
            int dotpos = videoName.lastIndexOf('.');
            f.setName(dotpos != -1 ? videoName.substring(0, dotpos) : videoName);
            List<Long> beats = beatBar.getAllBeats();
            f.setVideoPositions(beats);
            int figureId = persistenceProvider.persistFigure(f);

            // create pictures of positions

            File pictureDir = workspace.getPictureDir();
            new File(pictureDir.getAbsolutePath() + File.separator + figureId).mkdirs();

            for (int i = 0; i < beats.size() - 1; i++) {
                long thisBeat = beats.get(i);
                long nextBeat = beats.get(i + 1);
                for (int j = 0; j < 4; j++) {
                    long time = thisBeat + j * (nextBeat - thisBeat) / 4;
                    captureImage(pictureDir, figureId, time, i / 2 + 1, j + (i % 2) * 4 + 1);
                }
            }
            // final position
            int last = beats.size() - 1;
            captureImage(pictureDir, figureId, beats.get(last), last / 2 + 1, 1 + (last % 2) * 4);

            // after a successful import, sync the persistence and close the import window
            persistenceProvider.sync();
            frame.setVisible(false);

        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An IO problem occured: " + e.getLocalizedMessage());
        }
    }

    private void captureImage(File pictureDir, int figureId, long time, int bar, int beat) {
        mediaPlayer.setMediaTime(new Time(time));
        mediaPlayer.prefetch();
        mediaPlayer.waitForState(Controller.Prefetched);

        String pictureName = String.format("%03d-%d.jpg", bar, beat);
        captureImage(new File(pictureDir.getAbsolutePath() + File.separator + figureId + File.separator + pictureName));
    }

    private void captureImage(File targetFile) {
        Buffer frame = grabber.grabFrame();
        if (frame == null) {
            System.out.println("Capturing frame: no frame received.");
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
            System.out.println("An IO problem occured during saving of the picture: " + frame.getSequenceNumber()
                    + ".jpg");
            e.printStackTrace();
        }
    }

    /**
     * Sets the player to the given position. If the player was running it will be started again after repositioning.
     * 
     * @param nanos the position in time to where the video should be set
     */
    public void setVideoTime(long nanos) {
        boolean wasRunning = false;
        if (mediaPlayer.getPlayer().getState() == Controller.Started) {
            mediaPlayer.stop();
            wasRunning = true;
        }
        mediaPlayer.setMediaTime(new Time(nanos));
        if (wasRunning) {
            mediaPlayer.start();
        }
    }

    /**
     * @param persistenceProvider the persistenceProvider to set
     */
    public void setPersistenceProvider(PersistenceProvider persistenceProvider) {
        this.persistenceProvider = persistenceProvider;
    }

    /**
     * @param workspace the workspace to set
     */
    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public static void main(String[] args) throws IOException {
        Workspace w = new SimpleWorkspace(new File("figurantdata"));
        final PersistenceProvider pp = new XStreamPersistenceProvider(new File(w.getDatabaseDir() + File.separator
                + "objects.xml"));
        pp.open();
        // VideoImport panel = new VideoImport("file:/home/sberner/Desktop/10-21.04.09.flv", null);
        VideoImport panel = new VideoImport("file:/home/sberner/Desktop/10-31.03.09.flv", pp, w);
        final SimplePanelFrame frame = new SimplePanelFrame(panel, 800, 600);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    System.out.println("DEBUG: window closed");
                    pp.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Problem with persisting data.\n" + ex.getLocalizedMessage());
                }
            }
        });
    }
}
