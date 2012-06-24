/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 24, 2012
 */
package figurabia.persistence;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.xstream.XStream;

import figurabia.framework.simpleimpl.AbstractStore;
import figurabia.framework.store.Identifiable;
import figurabia.framework.store.StoreException;
import figurabia.framework.store.StoreListener.StateChange;

public class XStreamStore<T extends Identifiable> extends AbstractStore<T> {

    private final static String SUFFIX = ".xml";

    private final File basePath;
    private final XStream xstream;
    private final Class<T> type;
    private Map<Long, Long> currentRevisions = new HashMap<Long, Long>();
    private Map<Long, T> cache = new HashMap<Long, T>();
    private long idCounter;

    XStreamStore(XStream xstream, File basePath, Class<T> type) {
        this.basePath = basePath;
        this.xstream = xstream;
        this.type = type;
        idCounter = getHighestExistingId() + 1;
    }

    private long getHighestExistingId() {
        String[] files = basePath.list();
        long highest = 0;
        for (String f : files) {
            if (f.endsWith(".xml")) {
                String numString = f.substring(0, f.length() - 4);
                try {
                    long num = Long.parseLong(numString);
                    if (num > highest)
                        highest = num;
                } catch (NumberFormatException e) {
                    // just continue
                }
            }
        }
        return highest;
    }

    @Override
    public long create(T o) {
        long id = idCounter++;
        o.setId(id);
        o.setRev(0);
        store(o);
        notifyStoreListeners(StateChange.CREATED, o);
        return id;
    }

    @Override
    public void update(T o) {
        long id = o.getId();
        long rev = o.getRev();
        long currentRevision = getCurrentRevision(id);
        if (currentRevision != rev)
            throw new StoreException("Object is stale (rev " + rev + "). Newer revision in store: "
                    + currentRevision);
        o.setRev(rev + 1);
        try {
            store(o);
        } catch (RuntimeException e) {
            o.setRev(rev); // restore, to avoid autoincrementing through automatic retry (could lead to loss of updates)
            throw e;
        }
        notifyStoreListeners(StateChange.UPDATED, o);
        currentRevisions.put(id, rev);
    }

    @Override
    public T read(long id) {
        if (!cache.containsKey(id)) {
            return load(id);
        }
        return cache.get(id);
    }

    @Override
    public void delete(T o) {
        long id = o.getId();
        long currentRev = getCurrentRevision(id);
        if (o.getRev() == currentRev) {
            idToFile(id).delete();
            cache.remove(id);
            currentRevisions.remove(id);
        }
    }

    private T load(long id) {
        File file = idToFile(id);
        Reader r = null;
        try {
            new BufferedReader(new FileReader(file));
            return type.cast(xstream.fromXML(r));
        } catch (IOException e) {
            throw new StoreException("Could not load object with id " + id, e);
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void store(T o) {
        File file = idToFile(o.getId());
        Writer w = null;
        try {
            w = new BufferedWriter(new FileWriter(file));
            xstream.toXML(o, w);
        } catch (IOException e) {
            throw new StoreException("Could not store object with id " + o.getId() + " and rev " + o.getRev(), e);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private long getCurrentRevision(long id) {
        if (!currentRevisions.containsKey(id)) {
            T o = read(id);
            currentRevisions.put(id, o.getRev());
            return o.getRev();
        }
        return currentRevisions.get(id);
    }

    private File idToFile(long id) {
        return new File(basePath, Long.toString(id) + SUFFIX);
    }
}
