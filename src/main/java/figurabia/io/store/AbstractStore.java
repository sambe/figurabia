/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 24, 2012
 */
package figurabia.io.store;

import java.util.HashSet;
import java.util.Set;


public abstract class AbstractStore<T extends Identifiable> implements Store<T> {

    private Set<StoreListener> storeListeners = new HashSet<StoreListener>();

    /**
     * @see figurabia.io.store.Store#createOrUpdate(figurabia.io.store.Identifiable)
     */
    @Override
    public String createOrUpdate(T o) {
        if (o.getId() == null)
            return create(o);
        update(o);
        return o.getId();
    }

    /**
     * @see figurabia.io.store.Store#addStoreListener(figurabia.io.store.StoreListener)
     */
    @Override
    public void addStoreListener(StoreListener<T> l) {
        storeListeners.add(l);
    }

    /**
     * @see figurabia.io.store.Store#removeStoreListener(figurabia.io.store.StoreListener)
     */
    @Override
    public void removeStoreListener(StoreListener<T> l) {
        storeListeners.remove(l);
    }

    protected void notifyStoreListeners(StoreListener.StateChange change, T o) {
        for (StoreListener<T> l : storeListeners) {
            try {
                l.update(change, o);
            } catch (RuntimeException e) {
                System.err.println("Exception thrown in StoreListener " + l + ":");
                e.printStackTrace();
            }
        }
    }

}
