/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 19.10.2009
 */
package figurabia.ui.figureexplorer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import net.miginfocom.swing.MigLayout;
import figurabia.domain.Figure;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.framework.FigureModel;
import figurabia.framework.FigurePositionListener;
import figurabia.framework.PersistenceProvider;
import figurabia.framework.Workspace;
import figurabia.framework.simpleimpl.SimpleWorkspace;
import figurabia.persistence.XStreamPersistenceProvider;
import figurabia.ui.figureexplorer.PositionPossibilitiesView.FigureLinkActionListener;
import figurabia.ui.framework.Perspective;
import figurabia.ui.framework.PlayerListener;
import figurabia.ui.framework.PositionListener;
import figurabia.ui.util.SimplePanelFrame;
import figurabia.ui.video.FigurePlayer;
import figurabia.ui.video.engine.MediaPlayer;

@SuppressWarnings("serial")
public class FigureExplorerPerspective extends JPanel implements Perspective {

    private FigureModel figureModel;
    private FigurePlayer player;
    private FigurePositionsView positionsView;
    private PositionChooser positionChooser;
    private PositionPossibilitiesView possibilitiesView;

    public FigureExplorerPerspective(Workspace workspace, PersistenceProvider persistenceProvider,
            MediaPlayer mediaPlayer, FigureModel figureModel_) {
        figureModel = figureModel_;
        player = new FigurePlayer(workspace, mediaPlayer, figureModel_);
        positionsView = new FigurePositionsView(workspace);
        positionChooser = new PositionChooser();
        possibilitiesView = new PositionPossibilitiesView(workspace, persistenceProvider);

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

        player.addPlayerListener(new PlayerListener() {
            @Override
            public void positionActive(Figure f, int i) {
                positionsView.setPosition(f, i);
                if (i != -1) {
                    PuertoPosition p = f.getPositions().get(i);
                    positionChooser.setPosition(p, f.getCombinedOffset(i));
                }
            }
        });

        positionChooser.addPositionListener(new PositionListener() {
            @Override
            public void positionActive(PuertoPosition p, PuertoOffset combinedOffset, PuertoOffset offset) {
                possibilitiesView.setPosition(p, combinedOffset);
            }
        });

        positionsView.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selected = positionsView.getSelectedIndex();
                if (selected != -1) {
                    player.setPosition(selected);
                }
            }
        });

        possibilitiesView.addFigureLinkActionListener(new FigureLinkActionListener() {
            @Override
            public void linkActivated(Figure figure, int index) {
                if (figure != figureModel.getCurrentFigure()) {
                    figureModel.setCurrentFigure(figure, index);
                    // TODO this is only a rough fix: setting the restricted position range
                    player.setRepeatFigureOnly(true);
                } else {
                    player.setPosition(index);
                }
            }
        });

        figureModel.addFigurePositionListener(new FigurePositionListener() {
            @Override
            public void update(Figure f, int position) {
                positionsView.setPosition(f, position);
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
            positionsView.setPosition(figureModel.getCurrentFigure(), figureModel.getCurrentPosition());
            possibilitiesView.updateIndex();
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

        Workspace w = new SimpleWorkspace(new File("figurantdata"));
        final PersistenceProvider pp = new XStreamPersistenceProvider(new File(w.getDatabaseDir() + File.separator
                + "objects.xml"));
        pp.open();
        FigureModel figureModel = new FigureModel();
        FigureExplorerPerspective panel = new FigureExplorerPerspective(w, pp, new MediaPlayer(), figureModel);
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

        // TODO also test without this block
        Iterator<Figure> it = pp.getAllFigures().iterator();
        it.next();
        it.next();
        //it.next();
        //it.next();
        Figure f = it.next();
        figureModel.setCurrentFigure(f, 0);
    }
}
