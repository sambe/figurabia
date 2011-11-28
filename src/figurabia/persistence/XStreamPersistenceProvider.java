/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 30.01.2010
 */
package figurabia.persistence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XppDriver;

import figurabia.domain.Element;
import figurabia.domain.Figure;
import figurabia.domain.Folder;
import figurabia.domain.FolderItem;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.framework.simpleimpl.AbstractFilePersistenceProvider;
import figurabia.framework.simpleimpl.SimplePersistenceProvider;

public class XStreamPersistenceProvider extends AbstractFilePersistenceProvider {

    public XStreamPersistenceProvider(File serializedDataFile) {
        super(serializedDataFile);
    }

    public XStream createXStream() {
        XStream xstream = new XStream(new XppDriver());
        xstream.alias("Figure", Figure.class);
        xstream.alias("PuertoPosition", PuertoPosition.class);
        xstream.alias("PuertoOffset", PuertoOffset.class);
        xstream.alias("Element", Element.class);
        xstream.alias("Folder", Folder.class);
        xstream.alias("FigureReference", FigureReference.class);
        xstream.alias("FolderReference", FolderReference.class);
        xstream.omitField(Element.class, "initialPosition");
        xstream.omitField(Element.class, "finalPosition");
        xstream.omitField(Figure.class, "parent");
        xstream.omitField(FigureReference.class, "parent");
        xstream.omitField(FolderReference.class, "parent");
        xstream.omitField(Folder.class, "parent");
        return xstream;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void read(InputStream is) throws IOException {
        ObjectInputStream in = createXStream().createObjectInputStream(is);

        // read data
        try {
            figureIdCounter = (Integer) in.readObject();
            figuresById = (Map<Integer, Figure>) in.readObject();
            namedPositions = (Map<String, PuertoPosition>) in.readObject();
            folderItems = (Map<Folder, List<FolderItem>>) in.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        afterReadingFolderItems();

        preprocess();
    }

    @Override
    protected void write(OutputStream os) throws IOException {
        ObjectOutputStream out = createXStream().createObjectOutputStream(os);

        // write data
        out.writeObject(figureIdCounter);
        out.writeObject(figuresById);
        out.writeObject(namedPositions);
        out.writeObject(createFolderItemsPreparedForWrite());
        out.close(); // for closing tag
    }

    private Map<Folder, List<FolderItem>> createFolderItemsPreparedForWrite() {
        Map<Folder, List<FolderItem>> preparedFolderItems = new HashMap<Folder, List<FolderItem>>(folderItems.size());
        for (Entry<Folder, List<FolderItem>> e : folderItems.entrySet()) {
            List<FolderItem> updatedList = new ArrayList<FolderItem>(e.getValue());
            for (int i = 0; i < updatedList.size(); i++) {
                if (updatedList.get(i) instanceof Figure) {
                    Figure f = (Figure) updatedList.get(i);
                    updatedList.set(i, new FigureReference(f.getId()));
                } else {
                    Folder f = (Folder) updatedList.get(i);
                    updatedList.set(i, new FolderReference(f.getId()));
                }
            }
            preparedFolderItems.put(e.getKey(), updatedList);
        }
        return preparedFolderItems;
    }

    private void afterReadingFolderItems() {
        // restore folders by ID
        folderById = new HashMap<Integer, Folder>(folderItems.size());
        for (Folder f : folderItems.keySet()) {
            folderById.put(f.getId(), f);
        }

        // replace figure references with real figures
        for (Entry<Folder, List<FolderItem>> e : folderItems.entrySet()) {
            Folder folder = e.getKey();
            List<FolderItem> items = e.getValue();
            for (int i = 0; i < items.size(); i++) {
                // replace figure reference with figure
                if (items.get(i) instanceof FigureReference) {
                    FigureReference fRef = (FigureReference) items.get(i);
                    if (!figuresById.containsKey(fRef.getId()))
                        throw new IllegalStateException("folderItems contains reference to figure with id "
                                + fRef.getId() + " that does not exist.");
                    Figure f = figuresById.get(fRef.getId());
                    items.set(i, f);
                }
                if (items.get(i) instanceof FolderReference) {
                    FolderReference fRef = (FolderReference) items.get(i);
                    if (!folderById.containsKey(fRef.getId()))
                        throw new IllegalStateException("folderItems contains reference to folder with id "
                                + fRef.getId() + " that does not exist.");
                    Folder f = folderById.get(fRef.getId());
                    items.set(i, f);
                }
                // set parent reference
                items.get(i).setParent(folder);
            }
        }

        // correct root folder
        rootFolder = folderById.get(-1);

        // find figures that are not in any folder and add them to the root folder (should not happen normally)
        for (Figure f : figuresById.values()) {
            if (f.getParent() == null) {
                System.out.println("Figure " + f.getId() + " was not in any folder. Now added to root folder");
                folderItems.get(rootFolder).add(f);
                f.setParent(rootFolder);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] arg) throws Exception {
        // migrate data
        AbstractFilePersistenceProvider from = new SimplePersistenceProvider(
                new File("figurantdata/db/objects.old.xml"));
        from.open();
        XStreamPersistenceProvider to = new XStreamPersistenceProvider(new File("figurantdata/db/objects.new.xml"));
        to.open();
        System.out.println("Extract data..");

        to.figureIdCounter = getIntField(from, "figureIdCounter");
        to.figuresById = (Map) getField(from, "figuresById");
        to.namedPositions = (Map) getField(from, "namedPositions");

        System.out.println("Writing data...");
        to.close();
        System.out.println("Finished writing data");
    }

    private static int getIntField(Object obj, String name) throws Exception {
        Field field = AbstractFilePersistenceProvider.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(obj);
    }

    private static Object getField(Object obj, String name) throws Exception {
        Field field = AbstractFilePersistenceProvider.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(obj);
    }

    private void preprocess() {
        for (Figure f : figuresById.values()) {
            List<Element> elements = f.getElements();
            List<PuertoPosition> positions = f.getPositions();
            if (elements == null || positions == null)
                continue;
            for (int i = 0; i < elements.size(); i++) {
                Element e = elements.get(i);
                e.setInitialPosition(positions.get(i));
                e.setFinalPosition(positions.get(i + 1));
            }
        }
    }
}
