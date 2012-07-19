/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 22.03.2010
 */
package figurabia.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import figurabia.domain.Element;
import figurabia.domain.Figure;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.domain.TreeItem;
import figurabia.domain.TreeItem.ItemType;
import figurabia.io.FigureStore;
import figurabia.io.FiguresTreeStore;
import figurabia.io.VideoDir;
import figurabia.io.workspace.Workspace;
import figurabia.io.workspace.WorkspaceException;

public class FigureCreationService {

    private Workspace workspace;
    private FigureStore figureStore;
    private VideoDir videoDir;
    private FiguresTreeStore treeStore;

    public FigureCreationService(Workspace workspace, FigureStore figureStore, VideoDir videoDir,
            FiguresTreeStore treeStore) {
        this.workspace = workspace;
        this.figureStore = figureStore;
        this.videoDir = videoDir;
        this.treeStore = treeStore;
    }

    public Figure createNewFigure(File videofile) throws IOException {
        return createNewFigure(videofile, null, -1);
    }

    public Figure createNewFigure(File videoFile, TreeItem parent, int index) throws IOException {
        String videoId = videoDir.addVideo(videoFile);

        // creating figure
        Figure f = new Figure();
        f.setVideoName(videoId);
        f.setVideoPositions(new ArrayList<Long>());
        prepareFigure(f);

        figureStore.create(f);

        if (parent == null) {
            parent = treeStore.getRootFolder();
            index = -1;
        }
        // -1 means insert at the end
        if (index == -1) {
            index = parent.getChildIds().size();
        }
        TreeItem itemRef = new TreeItem(null, ItemType.ITEM, f.getName());
        itemRef.setRefId(f.getId());
        treeStore.create(itemRef);
        treeStore.insertItem(parent, index, itemRef);

        return f;
    }

    public void prepareFigure(Figure f) {
        // check if figure already has a name, if not set the filename without ending
        if (f.getName() == null) {
            String fileName = f.getVideoName();
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot == -1)
                lastDot = fileName.length();
            f.setName(fileName.substring(0, lastDot));
        }
        // check if figure already contains positions, otherwise create new ones
        if (f.getPositions() == null) {
            int n = f.getVideoPositions().size();
            List<PuertoPosition> positions = new ArrayList<PuertoPosition>(n);
            for (int i = 0; i < n; i++) {
                PuertoPosition p = PuertoPosition.getInitialPosition().withBeat(i % 2 == 0 ? 1 : 5);
                positions.add(p);
            }
            f.setPositions(positions);
        }
        // check if figure already contains elements, otherwise create new ones
        if (f.getElements() == null && f.getPositions().size() > 0) {
            List<PuertoPosition> positions = f.getPositions();
            int n = positions.size() - 1;
            List<Element> elements = new ArrayList<Element>(n);
            for (int i = 0; i < n; i++) {
                Element e = new Element();
                e.setInitialPosition(positions.get(i));
                e.setFinalPosition(positions.get(i + 1));
                e.setOffsetChange(PuertoOffset.getInitialOffset());
                elements.add(e);
            }
            f.setElements(elements);
        }
        if (f.getElements() == null && f.getPositions().size() == 0) {
            f.setElements(new ArrayList<Element>());
        }
        // check if figure already contains a base offset, otherwise create a new one
        if (f.getBaseOffset() == null) {
            f.setBaseOffset(PuertoOffset.getInitialOffset());
        }
        // check if figure already contains barIds, otherwise create new ones
        if (f.getBarIds() == null) {
            List<PuertoPosition> positions = f.getPositions();
            List<Integer> barIds = new ArrayList<Integer>();
            for (int i = 0; i < positions.size(); i++) {
                barIds.add(i / 2 + 1);
            }
            f.setBarIds(barIds);
        }
    }

    public void cloneFigure(TreeItem item) {
        if (item.getType() == ItemType.ITEM) {
            Figure f = figureStore.read(item.getRefId());
            String newName = JOptionPane.showInputDialog(null,
                    "Please enter a name for the cloned figure:", f.getName());
            if (newName != null && !newName.equals("")) {
                try {
                    Figure clone = f.clone();
                    clone.setId(null);
                    clone.setName(newName);
                    figureStore.create(clone);
                    String fromPath = "/pics/" + f.getId();
                    String toPath = "/pics/" + clone.getId();
                    workspace.copyPath(fromPath, toPath);
                } catch (WorkspaceException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null,
                            "IO Error occured while copying pictures of the cloned figure: " + ex.getLocalizedMessage());
                }
            }
        } else {
            throw new IllegalStateException("called cloneFigure with something else than a figure: " + item);
        }

    }

    public void createNewFolder(TreeItem parent, String name) {
        int index = 0;
        if (parent.getType() == ItemType.ITEM) {
            TreeItem newParent = treeStore.getParentFolder(parent);
            index = newParent.getChildIds().indexOf(parent.getId()) + 1;
            parent = newParent;
        }
        TreeItem newFolder = new TreeItem(null, ItemType.FOLDER, name);
        treeStore.create(newFolder);

        treeStore.insertItem(parent, index, newFolder);
    }
}
