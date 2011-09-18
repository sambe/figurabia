/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 23.07.2009
 */
package figurabia.ui.figureeditor;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import figurabia.domain.Figure;
import figurabia.framework.FigureListener;
import figurabia.framework.FigureModel;
import figurabia.framework.PersistenceProvider;
import figurabia.framework.Workspace;
import figurabia.framework.simpleimpl.SimpleWorkspace;
import figurabia.persistence.XStreamPersistenceProvider;
import figurabia.ui.framework.Perspective;
import figurabia.ui.util.SimplePanelFrame;
import figurabia.ui.video.engine.MediaPlayer;

@SuppressWarnings("serial")
public class FigureEditPerspective extends JPanel implements Perspective {

    private FigureModel figureModel;
    private FigureList figureList;
    private FigureEditor figureEditor;

    public FigureEditPerspective(Workspace workspace, PersistenceProvider persistenceProvider, MediaPlayer player,
            FigureModel figureModel_) {
        this.figureModel = figureModel_;
        figureList = new FigureList(workspace, persistenceProvider);
        figureEditor = new FigureEditor(workspace, persistenceProvider, player, figureModel_);

        setLayout(new MigLayout("ins 0", "[fill]", "[fill]"));
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, figureList, figureEditor);
        splitPane.setDividerSize(8);
        splitPane.setDividerLocation(0.2);
        add(splitPane, "push");
        //add(figureEditor, "push");
        //add(figureList, "west");

        figureList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (figureList.getSelectedFigure() != null)
                    figureModel.setCurrentFigure(figureList.getSelectedFigure());
            }
        });

        figureModel.addFigureListener(new FigureListener() {
            @Override
            public void update(ChangeType type, Figure figure) {
                figureList.setSelectedFigure(figure);
            }
        });

        setOpaque(true);
    }

    public void setFigure(Figure f, int i) {
        figureList.setSelectedFigure(f);
        if (i != -1)
            figureEditor.setPositionIndex(i);
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

        Workspace w = new SimpleWorkspace(new File("figurantdata"));
        final PersistenceProvider pp = new XStreamPersistenceProvider(new File(w.getDatabaseDir() + File.separator
                + "objects.xml"));
        pp.open();
        FigureEditPerspective panel = new FigureEditPerspective(w, pp, new MediaPlayer(), new FigureModel());
        final SimplePanelFrame frame = new SimplePanelFrame(panel, 1000, 720);
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

    @Override
    public String getPerspectiveId() {
        return "editPerspective";
    }
}
