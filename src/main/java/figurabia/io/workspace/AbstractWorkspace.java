/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 3, 2012
 */
package figurabia.io.workspace;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;

import figurabia.util.PrefixMap;
import figurabia.util.Trie;

public abstract class AbstractWorkspace implements Workspace {

    //private SortedMap<String, List<WorkspaceUpdateListener>> workspaceUpdateListeners = new TreeMap<String, List<WorkspaceUpdateListener>>();
    private PrefixMap<List<WorkspaceUpdateListener>> workspaceUpdateListeners = new Trie<List<WorkspaceUpdateListener>>();

    @Override
    public void addWorkspaceUpdateListener(String pathPrefix, WorkspaceUpdateListener l) {
        List<WorkspaceUpdateListener> listeners = workspaceUpdateListeners.get(pathPrefix);
        if (listeners == null) {
            listeners = new ArrayList<Workspace.WorkspaceUpdateListener>();
            workspaceUpdateListeners.put(pathPrefix, listeners);
        }
        listeners.add(l);
    }

    @Override
    public void removeWorkspaceUpdateListener(String pathPrefix, WorkspaceUpdateListener l) {
        List<WorkspaceUpdateListener> listeners = workspaceUpdateListeners.get(pathPrefix);
        if (listeners != null) {
            listeners.remove(l);
        }
    }

    protected void notifyWorkspaceUpdateListeners(String changedResourcePath, ChangeType change) {
        // find all listeners that were registered for a prefix of the changed resource path
        Collection<List<WorkspaceUpdateListener>> listeners = workspaceUpdateListeners.valuesAtPrefixOf(changedResourcePath);

        for (List<WorkspaceUpdateListener> list : listeners) {
            for (WorkspaceUpdateListener l : list) {
                try {
                    l.update(change, changedResourcePath);
                } catch (RuntimeException e) {
                    System.err.println("Error in WorkspaceUpdateListener");
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void finishedWriting(String path, boolean newResource) {
        notifyWorkspaceUpdateListeners(path, newResource ? ChangeType.CREATED : ChangeType.UPDATED);
    }

    @Override
    public InputStream read(String resourcePath) {
        File f = fileForReading(resourcePath);
        try {
            InputStream fis = new BufferedInputStream(new FileInputStream(f));
            return fis;
        } catch (FileNotFoundException e) {
            throw new WorkspaceException("Could not open InputStream to resource " + resourcePath, e);
        }
    }

    @Override
    public OutputStream write(String resourcePath) {
        File f = fileForWriting(resourcePath);
        try {
            OutputStream fos = new BufferedOutputStream(FileUtils.openOutputStream(f));
            return fos;
        } catch (IOException e) {
            throw new WorkspaceException("Could not open OutputStream to resource " + resourcePath, e);
        }
    }
}
