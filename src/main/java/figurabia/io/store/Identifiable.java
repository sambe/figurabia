/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 24, 2012
 */
package figurabia.io.store;

/**
 * All the requirements of a class to make it identifiable for storage.
 * 
 * @author Samuel Berner
 */
public interface Identifiable {

    /**
     * @return the ID, as created by the server
     */
    String getId();

    /**
     * @return the revision, to prevent data loss in update conflicts (optimistic locking)
     */
    String getRev();

    /**
     * Sets a new ID. Only to be used internally by a Store implementation.
     * 
     * @param id the new id
     */
    void setId(String id);

    /**
     * Sets the revision. Only to be used internally by a Store implementation.
     * 
     * @param rev the new revision
     */
    void setRev(String rev);
}
