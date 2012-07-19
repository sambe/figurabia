/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 30, 2012
 */
package figurabia.io.workspace;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * This interface handles the saving/retrieving of files
 * 
 * @author Samuel Berner
 */
public interface Workspace {

    enum ChangeType {
        CREATED, UPDATED, DELETED
    }

    /**
     * To be notified about changes in the workspace.
     * 
     * @author Samuel Berner
     */
    interface WorkspaceUpdateListener {
        void update(ChangeType type, String changedResourcePath);
    }

    /**
     * Register a listener to get updates only when a resource starting with pathPrefix is affected.
     * 
     * @param pathPrefix the prefix to match, can be an empty String to match everything
     * @param l the listener
     */
    void addWorkspaceUpdateListener(String pathPrefix, WorkspaceUpdateListener l);

    void removeWorkspaceUpdateListener(String pathPrevix, WorkspaceUpdateListener l);

    /**
     * After each successful writing operation it is mandatory to call this method or the workspace update listeners
     * won't receive an event.
     * 
     * @param path the created/updated path
     * @param newResource whether it was created (true) or just updated (false)
     */
    void finishedWriting(String path, boolean newResource);

    /**
     * Returns a list of resource paths for all the resources below a base path
     * 
     * @param basePath the base path
     * @return a list of resource paths for all files in the given basePath
     */
    List<String> list(String basePath);

    /**
     * Returns true if the resource exists.
     * 
     * @param resourcePath the path of the resource e.g. some_folder/some_file.txt
     * @return true if the resource exists
     */
    boolean exists(String resourcePath);

    /**
     * Moves a resource from one path to another.
     * 
     * @param oldPath the path, the resource has before the move
     * @param newPath the path it has after the move
     */
    void move(String oldPath, String newPath);

    /**
     * Creates a copy of a whole path with all its sub path resources.
     * 
     * @param path path to copy
     * @param copyPath new path for the copy
     */
    void copyPath(String path, String copyPath);

    /**
     * Creates and opens a readable stream to the given resource or throws an IOException if the given resource does not
     * exist.
     * 
     * @param resourcePath the path of the resource e.g. some_folder/some_file.txt
     * @return the already opened InputStream
     */
    InputStream read(String resourcePath);

    /**
     * Creates and opens a writable stream to the given resource or throws an IOException if it is not possible to
     * write.
     * 
     * @param resourcePath the path of the resource e.g. some_folder/some_file.txt
     * @return the already opened OutputStream
     */
    OutputStream write(String resourcePath);

    /**
     * Returns a file for reading and if necessary by the implementation it might do something else first, e.g.
     * downloading the file if it hasn't been downloaded yet.
     * 
     * @param resourcePath the path
     * @return the file
     */
    File fileForReading(String resourcePath);

    /**
     * Returns a file for writing (does not need to download a file that doesn't yet exist anyway).
     * 
     * @param resourcePath the path
     * @return the file
     */
    File fileForWriting(String resourcePath);

    /**
     * Deletes the resource at the given resource path
     * 
     * @param resourcePath given resource path
     */
    void delete(String resourcePath);
}
