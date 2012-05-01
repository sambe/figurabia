/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 20.07.2009
 */
package figurabia.framework.simpleimpl;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import figurabia.domain.Figure;
import figurabia.domain.PuertoPosition;

/**
 * Very simple persistence provider based on standard JDK bean-to-XML serialization.
 * 
 * @author Samuel Berner
 */
public class SimplePersistenceProvider extends AbstractFilePersistenceProvider {

    public SimplePersistenceProvider(File serializedDataFile) {
        super(serializedDataFile);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void read(InputStream is) throws IOException {
        XMLDecoder in = new XMLDecoder(is);

        // read data
        figureIdCounter = (Integer) in.readObject();
        figuresById = (Map<Integer, Figure>) in.readObject();
        namedPositions = (Map<String, PuertoPosition>) in.readObject();
    }

    @Override
    protected void write(OutputStream bos) throws IOException {
        XMLEncoder out = new XMLEncoder(bos);

        // write data
        out.writeObject(figureIdCounter);
        out.writeObject(figuresById);
        out.writeObject(namedPositions);
    }

}
