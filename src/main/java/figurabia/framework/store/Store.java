/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 24, 2012
 */
package figurabia.framework.store;

/**
 * Defines a CRUD interface for a repository. Please note that an implementation of this class does never attempt to
 * modify the objects to its new state (e.g. new id and/or revision). It also explicitly does NOT guarantee to keep
 * objects unique, but will create a fresh object each time it is loaded (so don't keep them around or register a
 * listener for changes).
 * 
 * @author Samuel Berner
 */
public interface Store<T extends Identifiable> {

    /**
     * Create a new object in the repository, with the new values from the given object (except id and rev). Please
     * note, this does NOT set the new id and the revision in the object.
     * 
     * @param o an object containing the initial values for this object
     * @return the new id of the new object (initial revision is 0)
     */
    long create(T o);

    /**
     * Updates the identified object in the repository with its new values. Please note, this does NOT update the
     * revision in the object.
     * 
     * @param o the object to update
     */
    void update(T o);

    /**
     * Create or update depending on whether the id is > 0 or not.
     * 
     * @param o object to create or update
     * @return the id of the object (useful in case it was new)
     */
    long createOrUpdate(T o);

    /**
     * Retrieve an object and return it.
     * 
     * @param id the id of the object to retrieve
     * @return the retrieved object
     */
    T read(long id);

    /**
     * Delete an object.
     * 
     * @param o the object to delete.
     */
    void delete(T o);

    /**
     * Adds a StoreListener.
     * 
     * @param l the listener
     */
    void addStoreListener(StoreListener<T> l);

    /**
     * Removes a StoreListener.
     * 
     * @param l the listener
     */
    void removeStoreListener(StoreListener<T> l);
}
