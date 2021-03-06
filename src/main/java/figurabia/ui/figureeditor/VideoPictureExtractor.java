/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 21.02.2010
 */
package figurabia.ui.figureeditor;

import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import exmoplay.engine.MediaPlayer;
import figurabia.domain.Element;
import figurabia.domain.Figure;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.framework.FigurabiaModel;
import figurabia.framework.FigureIndexListener;
import figurabia.io.BeatPictureCache;
import figurabia.io.FigureStore;
import figurabia.io.store.StoreListener;
import figurabia.ui.framework.PlayerListener;
import figurabia.ui.video.FigurePlayer;

@SuppressWarnings("serial")
public class VideoPictureExtractor extends JPanel {

    private final FigureStore figureStore;
    private final BeatPictureCache beatPictureCache;
    private final FigurabiaModel figurabiaModel;

    private FigurePlayer figurePlayer;
    private JButton new1Button;
    private JButton new5Button;
    private JButton correctButton;

    public VideoPictureExtractor(FigureStore fs, BeatPictureCache bpc, MediaPlayer player,
            FigurabiaModel fm) {
        this.figureStore = fs;
        this.beatPictureCache = bpc;
        this.figurabiaModel = fm;

        setLayout(new MigLayout("ins 0", "[][][]push[]", "[][]"));

        figurePlayer = new FigurePlayer(beatPictureCache, player, fm);
        add(figurePlayer, "span 4,grow,push,wrap");

        new1Button = new JButton("New 1");
        new1Button.setEnabled(false);
        add(new1Button, "");

        new5Button = new JButton("New 5");
        new5Button.setEnabled(false);
        add(new5Button, "");

        correctButton = new JButton("Correct Selected");
        correctButton.setEnabled(false);
        add(correctButton, "");

        // when the user selects a different figure
        figurabiaModel.addFigureIndexListener(new FigureIndexListener() {
            @Override
            public void update(Figure figure, int position, boolean figureChanged) {
                if (figureChanged) {
                    updateButtonsEnabled(figure);
                }
            }
        });

        // when the user changes the figure between active and inactive
        figureStore.addStoreListener(new StoreListener<Figure>() {
            @Override
            public void update(StateChange change, Figure o) {
                if (change == StateChange.UPDATED && o.equals(figurabiaModel.getCurrentFigure()))
                    updateButtonsEnabled(o);
            }
        });
    }

    public void addNewPosition(int beat) {
        long videoTime = figurePlayer.getVideoNanoseconds();
        Figure figure = figurabiaModel.getCurrentFigure();
        // find position to insert
        List<Long> videoPositions = figure.getVideoPositions();
        int pos = videoPositions.size();
        for (int i = 0; i < videoPositions.size(); i++) {
            if (videoTime < videoPositions.get(i)) {
                pos = i;
                break;
            }
        }

        // find new Bar ID
        int newBarId = 1000;
        for (int id : figure.getBarIds()) {
            if (id >= newBarId)
                newBarId = id + 1;
        }

        // insert
        PuertoPosition newPosition = PuertoPosition.getInitialPosition().withBeat(beat);
        Element newElement = new Element();
        newElement.setOffsetChange(PuertoOffset.getInitialOffset());
        // special case: first position
        if (figure.getPositions().size() == 0) {
            // do not add an element (the first one is the trailing position)
        }
        // special case: last position (replacing trailing position, creating element for previous trailing position)
        else if (pos == figure.getPositions().size()) {
            newElement.setInitialPosition(figure.getPositions().get(pos - 1));
            newElement.setFinalPosition(newPosition);
            figure.getElements().add(pos - 1, newElement);
        }
        // normal case: also adjusting the previous position if there is one
        else {
            if (pos > 0) {
                figure.getElements().get(pos - 1).setFinalPosition(newPosition);
            }
            newElement.setInitialPosition(newPosition);
            newElement.setFinalPosition(figure.getPositions().get(pos));
            figure.getElements().add(pos, newElement);
        }
        figure.getBarIds().add(pos, newBarId);
        figure.getVideoPositions().add(pos, videoTime);
        figure.getPositions().add(pos, newPosition);

        // add picture
        figurePlayer.captureCurrentImage(figure.getId(), newBarId, beat, videoTime);

        figureStore.update(figure);
    }

    public void correctSelectedPosition() {
        long time = figurePlayer.getVideoNanoseconds();
        Figure figure = figurabiaModel.getCurrentFigure();
        int index = figurePlayer.getCurrentIndex();
        figure.getVideoPositions().set(index, time);
        int bar = figure.getBarIds().get(index);
        int beat = figure.getPositions().get(index).getBeat();
        figurePlayer.captureCurrentImage(figure.getId(), bar, beat, time);

        figureStore.update(figure);
    }

    public void setPosition(int pos) {
        figurePlayer.setCurrentIndex(pos);
    }

    private void updateButtonsEnabled(Figure figure) {
        boolean enabled = !figure.isActive();
        new1Button.setEnabled(enabled);
        new5Button.setEnabled(enabled);
        correctButton.setEnabled(enabled);
    }

    public void setActive(boolean active) {
        figurePlayer.setActive(active);
    }

    /**
     * @param l
     * @see figurabia.ui.video.FigurePlayer#addPlayerListener(figurabia.ui.framework.PlayerListener)
     */
    public void addPlayerListener(PlayerListener l) {
        figurePlayer.addPlayerListener(l);
    }

    /**
     * @param l
     * @see figurabia.ui.video.FigurePlayer#removePlayerListener(figurabia.ui.framework.PlayerListener)
     */
    public void removePlayerListener(PlayerListener l) {
        figurePlayer.removePlayerListener(l);
    }

    /**
     * @param l
     * @see javax.swing.AbstractButton#addActionListener(java.awt.event.ActionListener)
     */
    public void addNew1ButtonActionListener(ActionListener l) {
        new1Button.addActionListener(l);
    }

    /**
     * @param l
     * @see javax.swing.AbstractButton#removeActionListener(java.awt.event.ActionListener)
     */
    public void removeNew1ButtonActionListener(ActionListener l) {
        new1Button.removeActionListener(l);
    }

    /**
     * @param l
     * @see javax.swing.AbstractButton#addActionListener(java.awt.event.ActionListener)
     */
    public void addNew5ButtonActionListener(ActionListener l) {
        new5Button.addActionListener(l);
    }

    /**
     * @param l
     * @see javax.swing.AbstractButton#removeActionListener(java.awt.event.ActionListener)
     */
    public void removeNew5ButtonActionListener(ActionListener l) {
        new5Button.removeActionListener(l);
    }

    /**
     * @param l
     * @see javax.swing.AbstractButton#addActionListener(java.awt.event.ActionListener)
     */
    public void addCorrectButtonActionListener(ActionListener l) {
        correctButton.addActionListener(l);
    }

    /**
     * @param l
     * @see javax.swing.AbstractButton#removeActionListener(java.awt.event.ActionListener)
     */
    public void removeCorrectButtonActionListener(ActionListener l) {
        correctButton.removeActionListener(l);
    }
}
