/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 24, 2012
 */
package figurabia.io.store;

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
    String create(T o);

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
    String createOrUpdate(T o);

    /**
     * Creates an object in the repository with the given id (if it does not exist already). Will return true if and
     * only if there was no object with this Id and it was created. If the ID exists, nothing will happen (except that
     * it returns false).
     * 
     * @param o the object to create in the repository
     * @return true if the object is created, false if the id was already taken (or create/update is not supported)
     */
    boolean createWithId(T o);

    /**
     * Retrieve an object and return it.
     * 
     * @param id the id of the object to retrieve
     * @return the retrieved object
     */
    T read(String id);

    /**
     * Delete an object.
     * 
     * @param o the object to delete.
     */
    void delete(T o);

    /**
     * Return true if the object with the given ID exists, otherwise false.
     * 
     * @param id the ID to check
     * @return true or false depending on whether the ID exists
     */
    boolean exists(String id);

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
