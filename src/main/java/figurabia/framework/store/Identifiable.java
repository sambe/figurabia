/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 24, 2012
 */
package figurabia.framework.store;

/**
 * All the requirements of a class to make it identifiable for storage.
 * 
 * @author Samuel Berner
 */
public interface Identifiable {

    /**
     * @return the ID, as created by the server
     */
    long getId();

    /**
     * @return the revision, to prevent data loss in update conflicts (optimistic locking)
     */
    long getRev();

    /**
     * Sets a new ID. Only to be used internally by a Store implementation.
     * 
     * @param id the new id
     */
    void setId(long id);

    /**
     * Sets the revision. Only to be used internally by a Store implementation.
     * 
     * @param rev the new revision
     */
    void setRev(long rev);
}
