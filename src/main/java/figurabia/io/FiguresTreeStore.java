/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 1, 2012
 */
package figurabia.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.xstream.XStream;

import figurabia.domain.TreeItem;
import figurabia.domain.TreeItem.ItemType;
import figurabia.io.store.StoreListener;
import figurabia.io.store.StoreListener.StateChange;
import figurabia.io.store.XStreamStore;
import figurabia.io.workspace.Workspace;

public class FiguresTreeStore extends XStreamStore<TreeItem> {

    private static final String ROOT_ID = "-1";

    private Map<String, TreeItem> parentFolder = new HashMap<String, TreeItem>();
    private Map<String, TreeItem> byRefId = new HashMap<String, TreeItem>();
    private Map<String, List<String>> childItems = new HashMap<String, List<String>>();
    private List<ParentChangeListener> parentChangeListeners = new ArrayList<FiguresTreeStore.ParentChangeListener>();

    protected TreeItem rootFolder;

    private static XStream getXStream() {
        XStream xstream = new XStream();
        xstream.alias("TreeItem", TreeItem.class);
        return xstream;
    }

    public FiguresTreeStore(Workspace workspace, String basePath) {
        super(getXStream(), workspace, basePath, TreeItem.class);

        for (TreeItem item : allObjects()) {
            for (String childId : item.getChildIds()) {
                parentFolder.put(childId, item);
            }
            if (item.getType() == ItemType.ITEM) {
                byRefId.put(item.getRefId(), item);
            }
            childItems.put(item.getId(), new ArrayList<String>(item.getChildIds()));
        }

        addStoreListener(new StoreListener<TreeItem>() {
            @Override
            public void update(figurabia.io.store.StoreListener.StateChange change, TreeItem o) {
                switch (change) {
                case CREATED:
                    // intentionally no break here
                case UPDATED:
                    if (o.getType() == ItemType.ITEM)
                        byRefId.put(o.getRefId(), o); // always needed to update stale copies with the newest
                    if (o.getType() == ItemType.FOLDER)
                        updateParentFolderForChildren(o);
                    if (o.getId().equals(ROOT_ID))
                        rootFolder = o;
                    break;
                case DELETED:
                    if (o.getType() == ItemType.ITEM)
                        byRefId.remove(o.getRefId());
                    // nothing to do for folder (should already be empty)
                    break;
                }
            }
        });

        rootFolder = read(ROOT_ID);
    }

    private void updateParentFolderForChildren(TreeItem item) {
        // find difference of change (childItems keeps "backup" for comparison)
        List<String> previousIds = childItems.get(item.getId());
        if (previousIds == null)
            previousIds = Collections.emptyList();
        Set<String> removedIds = new HashSet<String>(previousIds);
        removedIds.removeAll(item.getChildIds());
        Set<String> addedIds = new HashSet<String>(item.getChildIds());
        addedIds.removeAll(previousIds);

        // first remove, the ones that were deleted
        for (String childId : removedIds) {
            parentFolder.remove(childId);
            int index = previousIds.indexOf(childId);
            TreeItem child = read(childId);
            notifyParentChangeListener(StateChange.DELETED, item, index, child);
        }

        // then add/update all current ids (updating older to prevent stale objects in the parentFolder map)
        for (String childId : item.getChildIds()) {
            parentFolder.put(childId, item);
            int index = item.getChildIds().indexOf(childId);
            TreeItem child = read(childId);
            if (addedIds.contains(childId)) {
                notifyParentChangeListener(StateChange.CREATED, item, index, child);
            }
            // currently not sending out parentChange updated events (probably not needed, and potentially a big waste of performance)
        }

        // in case a child moved inside the list of children, the parent did not change (thus no notification)

        childItems.put(item.getId(), new ArrayList<String>(item.getChildIds()));
    }

    public interface ParentChangeListener {
        void update(StateChange change, TreeItem parent, int index, TreeItem child);
    }

    public void addParentChangeListener(ParentChangeListener l) {
        parentChangeListeners.add(l);
    }

    public void removeParentChangeListener(ParentChangeListener l) {
        parentChangeListeners.remove(l);
    }

    protected void notifyParentChangeListener(StateChange change, TreeItem parent, int index, TreeItem child) {
        for (ParentChangeListener l : parentChangeListeners) {
            try {
                l.update(change, parent, index, child);
            } catch (RuntimeException e) {
                System.err.println("Exception thrown in ParentChangeListener");
                e.printStackTrace();
            }
        }
    }

    public TreeItem getByRefId(String refId) {
        return byRefId.get(refId);
    }

    public TreeItem getParentFolder(TreeItem f) {
        return parentFolder.get(f.getId());
    }

    public TreeItem getRootFolder() {
        return rootFolder;
    }

    public void insertItem(TreeItem parent, int index, TreeItem child) {
        if (child.getId() == null)
            throw new IllegalArgumentException("child has an id of null (probably not created in store yet)");
        parent.getChildIds().add(index, child.getId());
        update(parent);
    }

    public void removeItem(TreeItem parent, int index) {
        parent.getChildIds().remove(index);
        update(parent);
    }

    public void moveItem(TreeItem itemToMove, TreeItem newParent, int newIndex) {
        TreeItem oldParent = parentFolder.get(itemToMove.getId());
        if (oldParent == null)
            throw new IllegalArgumentException("item does not have a parent, but it needs one to move");
        int oldIndex = oldParent.getChildIds().indexOf(itemToMove.getId());
        if (newIndex == -1)
            newIndex = 0;
        if (oldParent.equals(newParent)) {
            if (newIndex > oldIndex)
                newIndex--;
            oldParent.getChildIds().remove(oldIndex);
            newParent.getChildIds().add(newIndex, itemToMove.getId());
            update(newParent); // just one update needed
        } else {
            removeItem(oldParent, oldIndex);
            insertItem(newParent, newIndex, itemToMove);
        }
    }
}
