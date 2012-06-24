/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jun 24, 2012
 */
package figurabia.framework.simpleimpl;

import java.util.HashSet;
import java.util.Set;

import figurabia.framework.store.Identifiable;
import figurabia.framework.store.Store;
import figurabia.framework.store.StoreListener;

public abstract class AbstractStore<T extends Identifiable> implements Store<T> {

    private Set<StoreListener> storeListeners = new HashSet<StoreListener>();

    /**
     * @see figurabia.framework.store.Store#createOrUpdate(figurabia.framework.store.Identifiable)
     */
    @Override
    public long createOrUpdate(T o) {
        if (o.getId() <= 0)
            return create(o);
        update(o);
        return o.getId();
    }

    /**
     * @see figurabia.framework.store.Store#addStoreListener(figurabia.framework.store.StoreListener)
     */
    @Override
    public void addStoreListener(StoreListener<T> l) {
        storeListeners.add(l);
    }

    /**
     * @see figurabia.framework.store.Store#removeStoreListener(figurabia.framework.store.StoreListener)
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
