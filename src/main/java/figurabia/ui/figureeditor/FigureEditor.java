/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 23.07.2009
 */
package figurabia.ui.figureeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;
import exmoplay.engine.MediaPlayer;
import figurabia.domain.Element;
import figurabia.domain.Figure;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.framework.FigureModel;
import figurabia.framework.FigurePositionListener;
import figurabia.io.BeatPictureCache;
import figurabia.io.FigureStore;
import figurabia.io.workspace.Workspace;
import figurabia.service.FigureCreationService;
import figurabia.service.FigureUpdateService;
import figurabia.ui.framework.PlayerListener;
import figurabia.ui.framework.PositionListener;
import figurabia.ui.positionviewer.PositionDialogEditor;

@SuppressWarnings("serial")
public class FigureEditor extends JPanel {

    private PositionList positionList;
    private JPanel centerPanel;
    //private PositionPictureView pictureView;
    private VideoPictureExtractor pictureExtractor;
    private PositionDialogEditor dialogEditor;

    private FigureModel figureModel;

    private final FigureCreationService figureCreationService;

    private int selected = -1;
    private boolean inSetter = false;
    private boolean inPositionChangedAfterListSelect = false;

    public FigureEditor(Workspace workspace, FigureStore fs, BeatPictureCache bpc, MediaPlayer player,
            FigureModel fm, FigureCreationService fcs, FigureUpdateService fus) {
        this.figureModel = fm;
        this.figureCreationService = fcs;
        setLayout(new MigLayout("ins 0", "[fill]", "[fill]"));

        positionList = new PositionList(workspace, bpc, fus.createElementNames());
        positionList.setAutoscrolls(true);
        add(positionList, "south,gap 0 1");

        centerPanel = new JPanel();
        centerPanel.setLayout(new MigLayout("ins 0", "[fill]", "[fill]"));

        //pictureView = new PositionPictureView(workspace);
        pictureExtractor = new VideoPictureExtractor(workspace, fs, bpc, player, fm);
        dialogEditor = new PositionDialogEditor();
        //centerPanel.add(pictureView, "push,gap 0 1 0 6");
        centerPanel.add(pictureExtractor, "push,gap 0 1 0 6");
        centerPanel.add(dialogEditor, "east");

        add(centerPanel, "push");

        positionList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {

                // only process the final value change, not intermediate events
                if (e.getValueIsAdjusting())
                    return;

                dialogEditor.startCollectingUpdates();

                try {
                    selected = positionList.getSelectedIndex();
                    if (selected != -1) {
                        //pictureView.setPosition(figure, selected);
                        if (!inSetter)
                            pictureExtractor.setPosition(selected);

                        // set offset & base offset
                        Figure figure = figureModel.getCurrentFigure();
                        if (selected == 0) {
                            dialogEditor.setOffset(figure.getBaseOffset());
                            dialogEditor.setBaseOffset(PuertoOffset.getInitialOffset());
                        } else {
                            dialogEditor.setOffset(figure.getElements().get(selected - 1).getOffsetChange());

                            PuertoOffset baseOffset = figure.getBaseOffset();
                            for (int i = 0; i < selected - 1; i++) {
                                baseOffset = baseOffset.addOffset(figure.getElements().get(i).getOffsetChange());
                            }
                            dialogEditor.setBaseOffset(baseOffset);
                        }

                        // set position
                        dialogEditor.setPosition(figure.getPositions().get(selected));

                        // enable/disable CopyToNext button
                        dialogEditor.setCopyToNextEnabled(selected != figure.getPositions().size() - 1);
                    } else {
                        dialogEditor.setCopyToNextEnabled(false);
                    }
                } finally {
                    inPositionChangedAfterListSelect = true;
                    dialogEditor.finishCollectingUpdates();
                    inPositionChangedAfterListSelect = false;
                }
            }
        });

        dialogEditor.addPositionListener(new PositionListener() {
            @Override
            public void positionActive(PuertoPosition p, PuertoOffset offset, PuertoOffset offsetChange) {
                if (inPositionChangedAfterListSelect) // no real change, just changed the displayed position
                    return;
                // update position in figure
                int index = positionList.getSelectedIndex();
                setPositionInFigure(p, offsetChange, index);
            }
        });

        dialogEditor.addCopyToNextActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final PuertoPosition p = dialogEditor.getPosition();
                int index = positionList.getSelectedIndex();
                // we set a neutral offset, because we don't want to reapply the offset of the position before
                setPositionInFigure(p, PuertoOffset.getInitialOffset(), index + 1);
                positionList.setSelectedIndex(index + 1);
            }
        });

        pictureExtractor.addPlayerListener(new PlayerListener() {
            @Override
            public void positionActive(Figure f, int position) {
                // the figure can only change when the perspective was switched (other than from outside i.e. the figure list)
                if (figureModel.getCurrentFigure() != f) {
                    figureModel.setCurrentFigure(f, position);
                } else {
                    setPositionIndex(position);
                }
            }
        });

        figureModel.addFigurePositionListener(new FigurePositionListener() {
            @Override
            public void update(Figure figure, int position) {
                figureCreationService.prepareFigure(figure); // TODO move this to the appropriate place (creation)
                positionList.setFigure(figure);
            }
        });

        pictureExtractor.addNew1ButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pictureExtractor.addNewPosition(1);
                // FIXME this should be notified through backend
                positionList.updateList();
            }
        });
        pictureExtractor.addNew5ButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pictureExtractor.addNewPosition(5);
                // FIXME this should be notified through backend
                positionList.updateList();
            }
        });
        pictureExtractor.addCorrectButtonActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (positionList.getSelectedIndex() == -1)
                    return;
                pictureExtractor.correctSelectedPosition();
                // FIXME this should be updated indirectly via backend (listener on beatPictureCache)
                positionList.updatePicture(positionList.getSelectedIndex());
            }
        });
    }

    private void setPositionInFigure(PuertoPosition position, PuertoOffset offset, int index) {
        Figure f = figureModel.getCurrentFigure();
        PuertoPosition previousP = f.getPositions().get(index);
        // be careful not to change the beat of the target position
        position = position.withBeat(previousP.getBeat());
        f.getPositions().set(index, position);
        List<Element> elements = f.getElements();

        if (index != elements.size()) {
            Element e = elements.get(index);
            e.setInitialPosition(position);
        }
        if (index != 0) {
            Element prev = elements.get(index - 1);
            prev.setFinalPosition(position);
            prev.setOffsetChange(offset);
        } else {
            f.setBaseOffset(offset);
        }
    }

    public void setPositionIndex(int i) {
        inSetter = true;
        positionList.setSelectedIndex(i);
        inSetter = false;
    }

    public int getPositionIndex() {
        return positionList.getSelectedIndex();
    }

    public void setActive(boolean active) {
        pictureExtractor.setActive(active);
    }
}
