/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 19.10.2009
 */
package figurabia.ui.figureexplorer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import net.miginfocom.swing.MigLayout;
import exmoplay.engine.MediaPlayer;
import figurabia.domain.Figure;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.framework.FigurabiaModel;
import figurabia.framework.FigureIndexListener;
import figurabia.framework.PositionListener;
import figurabia.io.BeatPictureCache;
import figurabia.io.FigureStore;
import figurabia.io.workspace.LocalFileWorkspace;
import figurabia.io.workspace.Workspace;
import figurabia.ui.figureexplorer.PositionPossibilitiesView.FigureLinkActionListener;
import figurabia.ui.framework.Perspective;
import figurabia.ui.framework.PlayerListener;
import figurabia.ui.framework.PositionChangeListener;
import figurabia.ui.util.SimplePanelFrame;
import figurabia.ui.video.FigurePlayer;

@SuppressWarnings("serial")
public class FigureExplorerPerspective extends JPanel implements Perspective {

    private FigurabiaModel figurabiaModel;
    private FigurePlayer player;
    private FigurePositionsView positionsView;
    private PositionChooser positionChooser;
    private PositionPossibilitiesView possibilitiesView;

    public FigureExplorerPerspective(FigureStore fs, BeatPictureCache bpc,
            MediaPlayer mediaPlayer, FigurabiaModel fm) {
        figurabiaModel = fm;
        player = new FigurePlayer(bpc, mediaPlayer, figurabiaModel);
        positionsView = new FigurePositionsView(bpc);
        positionChooser = new PositionChooser();
        possibilitiesView = new PositionPossibilitiesView(fs, bpc, figurabiaModel);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new MigLayout("ins 0", "[fill]", "[fill]"));
        topPanel.add(positionChooser, "west");
        topPanel.add(player, "push"); //,gap 0 0 6 7
        topPanel.add(positionsView, "east,pushy");

        JScrollPane bottomPanel = new JScrollPane(possibilitiesView, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, topPanel, bottomPanel);
        splitPane.setDividerSize(8);
        setLayout(new MigLayout("ins 0", "[fill]", "[fill]"));
        add(splitPane, "push");

        figurabiaModel.addPositionListener(new PositionListener() {
            @Override
            public void update(PuertoPosition position, PuertoOffset offset) {
                positionChooser.setPosition(position, offset);
                possibilitiesView.setPosition(position, offset);
            }
        });

        figurabiaModel.addFigureIndexListener(new FigureIndexListener() {
            @Override
            public void update(Figure f, int position, boolean figureChanged) {
                positionsView.setPosition(f, position);
            }
        });

        player.addPlayerListener(new PlayerListener() {
            @Override
            public void update(Figure f, int i) {
                figurabiaModel.setCurrentFigure(f, i);
            }
        });

        positionChooser.addPositionListener(new PositionChangeListener() {
            @Override
            public void positionActive(PuertoPosition p, PuertoOffset combinedOffset, PuertoOffset offset) {
                figurabiaModel.setCurrentPosition(p, combinedOffset);
            }
        });

        positionsView.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selected = positionsView.getSelectedIndex();
                if (selected != -1) {
                    player.setCurrentIndex(selected);
                }
            }
        });

        possibilitiesView.addFigureLinkActionListener(new FigureLinkActionListener() {
            @Override
            public void linkActivated(Figure figure, int index) {
                if (figure != figurabiaModel.getCurrentFigure()) {
                    figurabiaModel.setCurrentFigure(figure, index);
                    // TODO this is only a rough fix: setting the restricted position range
                    player.setRepeatFigureOnly(true);
                } else {
                    player.setCurrentIndex(index);
                }
            }
        });

        // set a base position to the PositionChooser (to make it active from the beginning)
        positionChooser.setPosition(PuertoPosition.getInitialPosition(), PuertoOffset.getInitialOffset());
    }

    /**
     * Does whatever needs to be updated after a perspective switch to this perspective.
     */
    public void updateOnPerspectiveSwitch(boolean active) {
        System.out.println("DEBUG: FigureExplorerPerspective active = " + active);
        player.setActive(active);
        player.setRepeatFigureOnly(active);
        if (active) {
            positionsView.setPosition(figurabiaModel.getCurrentFigure(), figurabiaModel.getCurrentFigureIndex());
        }
    }

    @Override
    public String getPerspectiveId() {
        return "explorerPerspective";
    }

    public static void main(String[] args) throws IOException {
        //try {
        //    UIManager.setLookAndFeel(new GTKLookAndFeel());
        //} catch (UnsupportedLookAndFeelException e) {
        //    e.printStackTrace();
        //}

        Workspace w = new LocalFileWorkspace(new File("figurantdata"));
        FigureStore fs = new FigureStore(w, "/figures");
        BeatPictureCache bpc = new BeatPictureCache(w, "/pics");

        FigurabiaModel figureModel = new FigurabiaModel();
        FigureExplorerPerspective panel = new FigureExplorerPerspective(fs, bpc, new MediaPlayer(), figureModel);
        final SimplePanelFrame frame = new SimplePanelFrame(panel, 1000, 720);
    }
}
