/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 13, 2012
 */
package figurabia.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import figurabia.domain.Element;
import figurabia.domain.Figure;
import figurabia.domain.PuertoPosition;
import figurabia.domain.TreeItem;
import figurabia.domain.TreeItem.ItemType;
import figurabia.io.BeatPictureCache;
import figurabia.io.FigureStore;
import figurabia.io.FiguresTreeStore;
import figurabia.io.workspace.Workspace;

public class FigureUpdateService {

    private final Workspace workspace;
    private final FigureStore figureStore;
    private final FiguresTreeStore treeStore;
    private final BeatPictureCache beatPictureCache;

    public FigureUpdateService(Workspace workspace, FigureStore figureStore, FiguresTreeStore treeStore,
            BeatPictureCache beatPictureCache) {
        this.workspace = workspace;
        this.figureStore = figureStore;
        this.treeStore = treeStore;
        this.beatPictureCache = beatPictureCache;
    }

    public void setName(TreeItem item, String newName) {
        switch (item.getType()) {
        case FOLDER:
            item.setName(newName);
            treeStore.update(item);
            break;
        case ITEM:
            String figureId = item.getRefId();
            Figure f = figureStore.read(figureId);
            f.setName(newName);
            figureStore.update(f);
            item.setName(newName);
            treeStore.update(item);
            break;
        }
    }

    public boolean figureIsActive(String figureId) {
        Figure f = figureStore.read(figureId);
        return f.isActive();
    }

    public void setActive(TreeItem item, boolean active) throws IOException {
        if (item.getType() == ItemType.ITEM) {
            Figure f = figureStore.read(item.getRefId());
            if (active) {
                validateAndFinalizeFigure(f);
            }
            f.setActive(active);
            figureStore.update(f);
        } else {
            throw new IllegalStateException("called set figure active " + active
                    + " on something else than a figure: " + item);
        }

    }

    public void validateAndFinalizeFigure(Figure f) throws IOException {
        // check if alternating 1 and 5, starting with 1 and ending with 1
        boolean oneNext = true;
        for (PuertoPosition p : f.getPositions()) {
            if (oneNext && p.getBeat() != 1 || !oneNext && p.getBeat() != 5) {
                throw new IllegalArgumentException("figure does not consist of alternating 1 and 5");
            }
            oneNext = !oneNext;
        }
        if (f.getPositions().size() % 2 != 1) {
            throw new IllegalArgumentException("figure does not end with a 1");
        }

        // rearrange bar ids, rename files
        List<Integer> barIds = f.getBarIds();
        for (int i = 0; i < barIds.size(); i++) {
            int oldBarId = barIds.get(i);
            int newBarId = i / 2 + 1;
            if (oldBarId != newBarId) {
                moveBarId(f, oldBarId, newBarId, f.getPositions().get(i).getBeat());
                barIds.set(i, newBarId);
            }
        }

        // clear image cache to ensure that the right images are loaded
        clearImageCache(f);
    }

    private void moveBarId(Figure f, int oldBarId, int newBarId, int beat) throws IOException {
        String oldPath = beatPictureCache.getPicturePath(f.getId(), oldBarId, beat);
        String newPath = beatPictureCache.getPicturePath(f.getId(), newBarId, beat);
        if (workspace.exists(newPath)) {
            List<Integer> barIds = f.getBarIds();
            int index = barIds.indexOf(newBarId);
            if (index != -1 && f.getPositions().get(index).getBeat() != beat) {
                int subIndex = barIds.subList(index + 1, barIds.size()).indexOf(newBarId);
                if (subIndex == -1)
                    index = -1;
                else
                    index = index + 1 + subIndex;
            }
            if (index == -1) {
                // delete the picture because it is not referenced by any bar
                workspace.delete(newPath);
            } else {
                // swap new name out (should only be temporary)
                int backupBarId = 10000 + newBarId;
                String backupPath = beatPictureCache.getPicturePath(f.getId(), backupBarId, beat);
                workspace.move(oldPath, backupPath);
                barIds.set(index, backupBarId);
            }
        }
        workspace.move(oldPath, newPath);
    }

    private void clearImageCache(Figure f) {
        List<PuertoPosition> positions = f.getPositions();
        List<Integer> barIds = f.getBarIds();
        for (int i = 0; i < barIds.size(); i++) {
            beatPictureCache.removePictureFromCache(f.getId(), barIds.get(i), positions.get(i).getBeat());
        }
    }

    public void delete(TreeItem item) {
        if (item.getType() == ItemType.ITEM) {
            TreeItem parent = treeStore.getParentFolder(item);
            int index = parent.getChildIds().indexOf(item.getId());
            treeStore.removeItem(parent, index);
            treeStore.delete(item);
            Figure f = figureStore.read(item.getRefId());
            figureStore.delete(f);
            // TODO maybe delete video too
            try {
                beatPictureCache.deleteAllPictures(f.getId());
            } catch (IOException e1) {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(null, "IO Error occured while deleting pictures of the deleted figure: "
                        + e1.getLocalizedMessage());
            }
        } else if (item.getType() == ItemType.FOLDER) {
            if (item.getChildIds().size() > 0)
                throw new IllegalArgumentException("Currently only supports deleting empty folders.");
            TreeItem parent = treeStore.getParentFolder(item);
            int index = parent.getChildIds().indexOf(item.getId());
            treeStore.removeItem(parent, index);
            treeStore.delete(item);

        }
    }

    public Figure getFigure(TreeItem item) {
        if (item != null && item.getType() == ItemType.ITEM)
            return figureStore.read(item.getRefId());
        return null;
    }

    public List<Figure> getAllActiveFiguresInSubTree(TreeItem item) {
        List<Figure> list = new ArrayList<Figure>();
        getFiguresInSubTree(list, item, true);
        return list;
    }

    private void getFiguresInSubTree(List<Figure> list, TreeItem item, boolean activeOnly) {
        if (item.getType() == ItemType.ITEM) {
            Figure f = figureStore.read(item.getRefId());
            if (!activeOnly || f.isActive())
                list.add(f);
        } else {
            for (String id : item.getChildIds()) {
                TreeItem child = treeStore.read(id);
                getFiguresInSubTree(list, child, activeOnly);
            }
        }
    }

    public Set<String> createElementNames() {
        Set<String> names = new TreeSet<String>(Arrays.asList("", "360", "Ambulancia", "Caminata", "Copa",
                "Dile que no", "Doppeldrehung", "Dreifachdrehung", "Encuentro", "Enchufla", "Grundschritt",
                "Inside turn", "Lazo", "Manitos volandos", "Open break", "Outside turn", "Titanic"));

        // collect all element names in use
        for (Figure f : figureStore.getAllFigures()) {
            for (Element e : f.getElements()) {
                if (e != null && e.getName() != null)
                    names.add(e.getName());
            }
        }

        return names;
    }

}
