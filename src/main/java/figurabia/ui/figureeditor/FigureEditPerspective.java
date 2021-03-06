/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 23.07.2009
 */
package figurabia.ui.figureeditor;

import java.io.File;
import java.io.IOException;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

import net.miginfocom.swing.MigLayout;
import exmoplay.engine.MediaPlayer;
import figurabia.domain.Figure;
import figurabia.framework.FigurabiaModel;
import figurabia.framework.FigureIndexListener;
import figurabia.io.BeatPictureCache;
import figurabia.io.FigureStore;
import figurabia.io.FiguresTreeStore;
import figurabia.io.VideoDir;
import figurabia.io.VideoMetaDataStore;
import figurabia.io.workspace.LocalFileWorkspace;
import figurabia.io.workspace.Workspace;
import figurabia.service.FigureCreationService;
import figurabia.service.FigureUpdateService;
import figurabia.ui.framework.Perspective;
import figurabia.ui.util.SimplePanelFrame;

@SuppressWarnings("serial")
public class FigureEditPerspective extends JPanel implements Perspective {

    private FigurabiaModel figurabiaModel;
    private FigureList figureList;
    private FigureEditor figureEditor;

    public FigureEditPerspective(Workspace workspace, FigureStore fs, FiguresTreeStore treeStore, BeatPictureCache bpc,
            FigureCreationService fcs, FigureUpdateService fus, MediaPlayer player,
            FigurabiaModel figurabiaModel_) {
        this.figurabiaModel = figurabiaModel_;
        figureList = new FigureList(treeStore, fcs, fus, figurabiaModel_);
        figureEditor = new FigureEditor(workspace, fs, bpc, player, figurabiaModel_, fcs, fus);

        setLayout(new MigLayout("ins 0", "[fill]", "[fill]"));
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, figureList, figureEditor);
        splitPane.setDividerSize(8);
        splitPane.setDividerLocation(0.2);
        add(splitPane, "push");

        figureList.addTreeSelectionListener(new TreeSelectionListener() {

            @Override
            public void valueChanged(TreeSelectionEvent e) {
                Figure selectedFigure = figureList.getSelectedFigure();
                if (selectedFigure != null) {
                    int initialPosition = -1;
                    if (selectedFigure.getVideoPositions().size() > 0)
                        initialPosition = 0;
                    figurabiaModel.setCurrentFigure(selectedFigure, initialPosition);
                }
            }
        });

        figurabiaModel.addFigureIndexListener(new FigureIndexListener() {
            @Override
            public void update(Figure figure, int position, boolean figureChanged) {
                if (figureChanged) {
                    figureList.setSelectedFigure(figure);
                }
            }
        });

        setOpaque(true);
    }

    public Figure getFigure() {
        return figureList.getSelectedFigure();
    }

    public int getPositionIndex() {
        return figureEditor.getPositionIndex();
    }

    /**
     * Does whatever needs to be updated after a perspective switch to this perspective.
     */
    public void updateOnPerspectiveSwitch(boolean active) {
        System.out.println("DEBUG: FigureEditPerspective active = " + active);
        figureEditor.setActive(active);
    }

    public static void main(String[] args) throws IOException {
        //try {
        //    UIManager.setLookAndFeel(new GTKLookAndFeel());
        //} catch (UnsupportedLookAndFeelException e) {
        //    e.printStackTrace();
        //}

        Workspace w = new LocalFileWorkspace(new File("figurantdata"));
        FigureStore fs = new FigureStore(w, "/figures");
        FiguresTreeStore fts = new FiguresTreeStore(w, "/tree");
        BeatPictureCache bpc = new BeatPictureCache(w, "/pics");
        VideoMetaDataStore vmds = new VideoMetaDataStore(w, "/vids/meta");
        VideoDir videoDir = new VideoDir(w, "/vids", vmds);

        FigureEditPerspective panel = new FigureEditPerspective(w, fs, fts, bpc, new FigureCreationService(w, fs,
                videoDir,
                fts), new FigureUpdateService(w, fs, fts, bpc), new MediaPlayer(), new FigurabiaModel());
        final SimplePanelFrame frame = new SimplePanelFrame(panel, 1000, 720);
    }

    @Override
    public String getPerspectiveId() {
        return "editPerspective";
    }
}
