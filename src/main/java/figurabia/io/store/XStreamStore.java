/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 24, 2012
 */
package figurabia.io.store;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.thoughtworks.xstream.XStream;

import figurabia.io.store.StoreListener.StateChange;
import figurabia.io.workspace.Workspace;
import figurabia.io.workspace.Workspace.ChangeType;
import figurabia.io.workspace.Workspace.WorkspaceUpdateListener;

public class XStreamStore<T extends Identifiable> extends AbstractStore<T> {

    private final static String SUFFIX = ".xml";

    private final Workspace workspace;
    private final String basePath;
    private final XStream xstream;
    private final Class<T> type;
    private Map<String, String> currentRevisions = new HashMap<String, String>();
    private Map<String, T> cache = new HashMap<String, T>();
    private long idCounter;

    public XStreamStore(XStream xstream, Workspace workspace, String basePath, Class<T> type) {
        this.workspace = workspace;
        this.basePath = basePath;
        this.xstream = xstream;
        this.type = type;
        idCounter = getHighestExistingId() + 1;

        // add workspace listener
        workspace.addWorkspaceUpdateListener(basePath, new WorkspaceUpdateListener() {
            @Override
            public void update(ChangeType type, String changedResourcePath) {
                String id = pathToId(changedResourcePath);
                switch (type) {
                case CREATED:
                    T o1 = load(id);
                    notifyStoreListeners(StateChange.CREATED, o1);
                    break;
                case UPDATED:
                    T o2 = load(id);
                    notifyStoreListeners(StateChange.UPDATED, o2);
                    break;
                case DELETED:
                    T o3 = cache.remove(id);
                    currentRevisions.remove(id);
                    notifyStoreListeners(StateChange.DELETED, o3);
                    break;
                }
            }
        });

        // prefill cache
        List<String> objectPaths = workspace.list(basePath);
        for (String path : objectPaths) {
            if (path.endsWith(SUFFIX)) {
                String id = pathToId(path);
                load(id);
            }
        }
    }

    private long getHighestExistingId() {
        List<String> resources = workspace.list(basePath);
        long highest = 0;
        for (String r : resources) {
            if (r.endsWith(SUFFIX)) {
                String numString = r.substring(0, r.length() - 4);
                try {
                    long num = Long.valueOf(numString);
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
    public String create(T o) {
        String id = Long.toString(idCounter++);
        o.setId(id);
        o.setRev("0");
        store(o);
        return id;
    }

    @Override
    public void update(T o) {
        String id = o.getId();
        String rev = o.getRev();
        String currentRevision = getCurrentRevision(id);
        if (currentRevision != rev)
            throw new StoreException("Object is stale (rev " + rev + "). Newer revision in store: "
                    + currentRevision);
        o.setRev(Long.toString(Long.valueOf(rev) + 1));
        try {
            store(o);
        } catch (RuntimeException e) {
            o.setRev(rev); // restore, to avoid autoincrementing through automatic retry (could lead to loss of updates)
            throw e;
        }
    }

    @Override
    public T read(String id) {
        if (!cache.containsKey(id)) {
            return load(id);
        }
        return cache.get(id);
    }

    @Override
    public void delete(T o) {
        String id = o.getId();
        String currentRev = getCurrentRevision(id);
        if (o.getRev().equals(currentRev)) {
            workspace.delete(idToPath(id));
        } else
            throw new StoreException("Object is stale (rev " + o.getRev() + "). Newer revision in store: "
                    + currentRev);
    }

    private T load(String id) {
        String path = idToPath(id);
        Reader r = null;
        try {
            r = new InputStreamReader(workspace.read(path));
            T object = type.cast(xstream.fromXML(r));
            postLoad(object);
            cache.put(id, object);
            currentRevisions.put(id, object.getRev());
            return object;
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
        preStore(o);
        String path = idToPath(o.getId());
        Writer w = null;
        try {
            w = new OutputStreamWriter(workspace.write(path));
            xstream.toXML(o, w);
        } finally {
            IOUtils.closeQuietly(w);
        }
    }

    private String getCurrentRevision(String id) {
        if (!currentRevisions.containsKey(id)) {
            T o = read(id);
            currentRevisions.put(id, o.getRev());
            return o.getRev();
        }
        return currentRevisions.get(id);
    }

    private String idToPath(String id) {
        return basePath + "/" + id + SUFFIX;
    }

    private String pathToId(String path) {
        if (path.startsWith(basePath)) {
            String numStr = path.substring(basePath.length() + 1, path.length() - SUFFIX.length());
            return numStr;
        }
        throw new IllegalStateException("not inside basePath(" + basePath + "): " + path);
    }

    protected Collection<T> allObjects() {
        return cache.values();
    }

    @Override
    public boolean createWithId(T o) {
        String path = idToPath(o.getId());
        if (workspace.exists(path)) {
            return false;
        } else {
            o.setRev("0");
            store(o);
            return true;
        }
    }

    /**
     * This can be overriden to do some processing before storing each object.
     * 
     * @param o the object that's going to be stored next
     */
    protected void preStore(T o) {
    }

    /**
     * This can be overriden to do some processing after loading each object.
     * 
     * @param o the object that's going to be loaded next
     */
    protected void postLoad(T o) {
    }
}
