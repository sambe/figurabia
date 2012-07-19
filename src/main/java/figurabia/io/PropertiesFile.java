/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 1, 2012
 */
package figurabia.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import figurabia.io.workspace.Workspace;
import figurabia.io.workspace.Workspace.ChangeType;
import figurabia.io.workspace.Workspace.WorkspaceUpdateListener;

public class PropertiesFile {

    private final Workspace workspace;
    private final String path;
    private final Properties properties;

    public PropertiesFile(Workspace workspace, String path) {
        this.workspace = workspace;
        this.path = path;
        properties = new Properties();

        workspace.addWorkspaceUpdateListener(path, new WorkspaceUpdateListener() {
            @Override
            public void update(ChangeType type, String changedResourcePath) {
                load();
            }
        });

        load();
    }

    public String getProperty(String key) {
        return (String) properties.get(key);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        save();
    }

    private void load() {
        InputStream is = null;
        try {
            is = workspace.read(path);
            properties.load(is);
        } catch (IOException e) {
            throw new IllegalArgumentException("error in loading properties", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void save() {
        OutputStream os = null;
        boolean existed = workspace.exists(path);
        try {
            os = workspace.write(path);
            properties.store(os, null);
        } catch (IOException e) {
            throw new IllegalStateException("error in saving properties", e);
        } finally {
            IOUtils.closeQuietly(os);
        }
        workspace.finishedWriting(path, !existed);
    }
}
