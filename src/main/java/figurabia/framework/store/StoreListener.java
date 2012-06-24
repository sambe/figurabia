/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 24, 2012
 */
package figurabia.framework.store;

public interface StoreListener<T extends Identifiable> {

    public enum StateChange {
        CREATED, UPDATED, DELETED
    }

    void update(StateChange change, T o);
}
