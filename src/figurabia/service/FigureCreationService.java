/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 22.03.2010
 */
package figurabia.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import figurabia.domain.Element;
import figurabia.domain.Figure;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.framework.PersistenceProvider;
import figurabia.framework.Workspace;

public class FigureCreationService {

    private Workspace workspace;
    private PersistenceProvider persistenceProvider;

    public FigureCreationService(Workspace workspace, PersistenceProvider persistenceProvider) {
        this.workspace = workspace;
        this.persistenceProvider = persistenceProvider;
    }

    public Figure createNewFigure(File videoFile) throws IOException {
        // copying video file to workspace
        File destFile = new File(workspace.getVideoDir() + File.separator + videoFile.getName());
        int runningNumber = 1;
        boolean copied = false;
        while (!copied) {
            if (!destFile.exists()) {
                FileUtils.copyFile(videoFile, destFile);
                copied = true;
            } else if (FileUtils.contentEquals(videoFile, destFile)) {
                // we just reference the already existing file (no duplicate needed)
                copied = true;
            } else {
                // generate a new name
                String baseName = videoFile.getName();
                String extension = "";
                int dotPos = baseName.lastIndexOf('.');
                if (dotPos != -1) {
                    baseName = baseName.substring(0, dotPos);
                    extension = baseName.substring(dotPos);
                }
                String newName = baseName + "_" + String.format("%02d", runningNumber) + extension;
                destFile = new File(workspace.getVideoDir() + File.separator + newName);
                runningNumber++;
            }
        }

        // creating figure
        Figure f = new Figure();
        f.setVideoName(destFile.getName());
        f.setVideoPositions(new ArrayList<Long>());
        prepareFigure(f);

        int id = persistenceProvider.persistFigure(f);

        // creating figure pictures directory
        new File(workspace.getPictureDir() + File.separator + id).mkdir();

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
        String figurePrefix = workspace.getPictureDir().getAbsolutePath();
        File oldFile = new File(figurePrefix + workspace.getPictureName(f.getId(), oldBarId, beat));
        File newFile = new File(figurePrefix + workspace.getPictureName(f.getId(), newBarId, beat));
        if (newFile.exists()) {
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
                newFile.delete();
            } else {
                // swap new name out (should only be temporary)
                int backupBarId = 10000 + newBarId;
                File backupFile = new File(figurePrefix + workspace.getPictureName(f.getId(), backupBarId, beat));
                FileUtils.moveFile(newFile, backupFile);
                barIds.set(index, backupBarId);
            }
        }
        FileUtils.moveFile(oldFile, newFile);
    }

    private void clearImageCache(Figure f) {
        List<PuertoPosition> positions = f.getPositions();
        List<Integer> barIds = f.getBarIds();
        for (int i = 0; i < barIds.size(); i++) {
            workspace.removePictureFromCache(f.getId(), barIds.get(i), positions.get(i).getBeat());
        }
    }
}
