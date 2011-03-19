/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 30.01.2010
 */
package figurabia.framework.simpleimpl;

import java.beans.XMLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import figurabia.domain.Element;
import figurabia.domain.Figure;
import figurabia.domain.PuertoPosition;
import figurabia.framework.FigureListener;
import figurabia.framework.PersistenceProvider;
import figurabia.framework.FigureListener.ChangeType;

public abstract class AbstractFilePersistenceProvider implements PersistenceProvider {
    private File serializedDataFile;

    protected int figureIdCounter = 1;

    private boolean opened = false;

    protected Map<Integer, Figure> figuresById = new HashMap<Integer, Figure>();

    private Set<Figure> activeFigures = new HashSet<Figure>();

    protected Map<String, PuertoPosition> namedPositions = new HashMap<String, PuertoPosition>();

    protected Set<FigureListener> figureListeners = new HashSet<FigureListener>();

    public AbstractFilePersistenceProvider(File serializedDataFile) {
        this.serializedDataFile = serializedDataFile;
    }

    private int getNewId() {
        return figureIdCounter++;
    }

    @Override
    public void deleteElement(Element element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deleteFigure(Figure figure) {
        figuresById.remove(figure.getId());
        // remove anyway to be on the safe side (not only if figure is active)
        activeFigures.remove(figure);

        updateFigureListeners(ChangeType.FIGURE_REMOVED, figure);
    }

    @Override
    public Collection<Element> getAllElements() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Collection<Figure> getAllFigures() {
        return Collections.unmodifiableCollection(figuresById.values());
    }

    @Override
    public Collection<Figure> getAllActiveFigures() {
        return Collections.unmodifiableCollection(activeFigures);
    }

    @Override
    public Figure getFigureById(int id) {
        return figuresById.get(id);
    }

    @Override
    public void persistElement(Element element) {
        // TODO Auto-generated method stub

    }

    @Override
    public int persistFigure(Figure figure) {
        if (figure.getId() != 0)
            throw new IllegalArgumentException("the given figure already has an ID, use update instead");
        int id = getNewId();
        figure.setId(id);
        figuresById.put(id, figure);
        if (figure.isActive())
            activeFigures.add(figure);

        updateFigureListeners(ChangeType.FIGURE_ADDED, figure);
        return id;
    }

    @Override
    public void updateElement(Element element) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateFigure(Figure figure) {
        if (figure.getId() == 0)
            throw new IllegalArgumentException("the given figure has no ID, use persist instead");
        figuresById.put(figure.getId(), figure);
        if (figure.isActive())
            activeFigures.add(figure);

        updateFigureListeners(ChangeType.FIGURE_CHANGED, figure);
    }

    @Override
    public Figure cloneFigure(Figure figure) {
        Figure clone = figure.clone();
        clone.setId(0);
        persistFigure(clone);
        return clone;
    }

    @Override
    public void addNamedPosition(String name, PuertoPosition position) {
        namedPositions.put(name, position);
    }

    @Override
    public void deleteNamedPosition(String name) {
        namedPositions.remove(name);
    }

    @Override
    public Map<String, PuertoPosition> getNamedPositions() {
        return Collections.unmodifiableMap(namedPositions);
    }

    @Override
    public void addFigureChangeListener(FigureListener listener) {
        if (figureListeners.contains(listener))
            throw new IllegalArgumentException("listener already registered");
        figureListeners.add(listener);
    }

    @Override
    public void removeFigureChangeListener(FigureListener listener) {
        if (!figureListeners.contains(listener))
            throw new IllegalArgumentException("no such listener registered");
        figureListeners.remove(listener);
    }

    protected void updateFigureListeners(ChangeType type, Figure figure) {
        for (FigureListener l : figureListeners) {
            try {
                l.update(type, figure);
            } catch (RuntimeException e) {
                System.err.println("Exception thrown in FigureListener (" + type + "," + figure + ")");
                e.printStackTrace();
            }
        }
    }

    private void validate() {
        for (Figure f : figuresById.values()) {
            int positions = f.getPositions().size();
            if (f.getVideoPositions() == null)
                throw new IllegalStateException("figure " + f.toString() + " has no video positions (null)");
            if (f.getVideoPositions().size() != positions) {
                throw new IllegalStateException("figure " + f.toString() + " has " + f.getVideoPositions().size()
                        + " video positions when it should have " + positions);
            }
            if (f.getElements() == null)
                throw new IllegalStateException("figure " + f.toString() + " has no elements (null)");
            if (f.getElements().size() != 0 && f.getElements().size() != positions - 1)
                throw new IllegalStateException("figure " + f.toString() + " has " + f.getElements().size()
                        + " elements when it should have " + (positions - 1));
            if (f.getBarIds() == null)
                throw new IllegalStateException("figure " + f.toString() + " has no barIds (null)");
            if (f.getBarIds().size() != positions)
                throw new IllegalStateException("figure " + f.toString() + " has " + f.getBarIds().size()
                        + " barIds when it should have " + positions);

            //TODO possible validations: for active figures: 1-5 alternating beats & barIds regular 11223344...etc.
        }
    }

    protected abstract void read(InputStream is) throws IOException;

    protected abstract void write(OutputStream os) throws IOException;

    @Override
    public void open() throws IOException {
        if (serializedDataFile.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(serializedDataFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                read(bis);
                validate();

            } finally {
                try {
                    fis.close(); // make sure to close an opened file
                } catch (IOException ex) { // ignore, to prevent masking the original exception
                }
            }
        }
        opened = true;
        init();
    }

    private void init() {
        // initialize list of active figures
        activeFigures = new HashSet<Figure>();
        for (Figure f : getAllFigures()) {
            if (f.isActive()) {
                activeFigures.add(f);
            }
        }
    }

    @Override
    public void close() throws IOException {
        sync();
    }

    @Override
    public void sync() throws IOException {
        if (!opened)
            throw new IllegalStateException("The open() method has never been called.");
        // if last saved more than a day ago, backup the existing copy first
        File backupTargetFile = getBackupTargetFile(serializedDataFile);
        if (!backupTargetFile.exists() && serializedDataFile.exists()) {
            FileUtils.moveFile(serializedDataFile, backupTargetFile);
        }
        FileOutputStream fos = null;
        XMLEncoder out = null;
        try {
            fos = new FileOutputStream(serializedDataFile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            write(bos);
            bos.flush();

        } finally {
            if (out != null) {
                out.close(); // cannot throw IOException (but closes inner stream)
            } else if (fos != null) {
                try {
                    fos.close(); // make sure to close an opened file
                } catch (IOException ex) { // ignore, to prevent masking the original exception
                }
            }
        }
    }

    private static File getBackupTargetFile(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        String formattedDate = String.format("_%1$ty%1$tm%1$td", file.lastModified());
        String newName = name.substring(0, lastDot) + formattedDate + name.substring(lastDot);
        return new File(file.getParentFile(), newName);
    }
}
