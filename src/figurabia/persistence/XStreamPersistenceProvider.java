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
import java.util.List;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.XppDriver;

import figurabia.domain.Element;
import figurabia.domain.Figure;
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
        xstream.omitField(Element.class, "initialPosition");
        xstream.omitField(Element.class, "finalPosition");
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
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        preprocess();
    }

    @Override
    protected void write(OutputStream os) throws IOException {
        ObjectOutputStream out = createXStream().createObjectOutputStream(os);

        // write data
        out.writeObject(figureIdCounter);
        out.writeObject(figuresById);
        out.writeObject(namedPositions);
        out.close(); // for closing tag
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
