/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 13.11.2011
 */
package figurabia.ui.figureeditor;

import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.jdesktop.swingx.tree.TreeModelSupport;

import figurabia.domain.TreeItem;
import figurabia.domain.TreeItem.ItemType;
import figurabia.io.FiguresTreeStore;
import figurabia.io.FiguresTreeStore.ParentChangeListener;
import figurabia.io.store.StoreListener;
import figurabia.io.store.StoreListener.StateChange;

public class FiguresTreeModel implements TreeModel {

    private final TreeModelSupport treeModelSupport = new TreeModelSupport(this);
    private final FiguresTreeStore figuresTreeStore;

    public FiguresTreeModel(FiguresTreeStore fts) {
        this.figuresTreeStore = fts;

        figuresTreeStore.addStoreListener(new StoreListener<TreeItem>() {
            @Override
            public void update(figurabia.io.store.StoreListener.StateChange change, TreeItem item) {
                switch (change) {
                case CREATED:
                    // not doing anything, because it cannot yet be referenced anywhere in the tree ([1] create item, [2] add its id to one already in the tree)
                    break;
                case UPDATED:
                    TreeItem parent = figuresTreeStore.getParentFolder(item);
                    if (parent == null) {
                        treeModelSupport.fireNewRoot();
                    } else {
                        TreePath parentPath = createTreePath(parent);
                        int index = parent.getChildIds().indexOf(item.getId());
                        treeModelSupport.fireChildChanged(parentPath, index, item);
                    }
                    break;
                case DELETED:
                    // doing nothing because, the event was already sent when it was removed from the folder ([1] remove id from folder, [2] remove item from store)
                    break;
                }
            }
        });
        figuresTreeStore.addParentChangeListener(new ParentChangeListener() {
            @Override
            public void update(StateChange change, TreeItem parent, int index, TreeItem child) {
                TreePath parentPath = createTreePath(parent);
                switch (change) {
                case CREATED:
                    treeModelSupport.fireChildAdded(parentPath, index, child);
                    break;
                case UPDATED:
                    treeModelSupport.fireChildChanged(parentPath, index, child);
                    break;
                case DELETED:
                    treeModelSupport.fireChildRemoved(parentPath, index, child);
                    break;
                }
            }
        });
    }

    private Object[] createTreePathArray(TreeItem item, int n) {
        if (item == null)
            return new Object[n];
        Object[] array = createTreePathArray(figuresTreeStore.getParentFolder(item), n + 1);
        array[array.length - 1 - n] = item;
        return array;
    }

    public TreePath createTreePath(TreeItem item) {
        Object[] treePathArray = createTreePathArray(item, 0);
        return new TreePath(treePathArray);
    }

    @Override
    public Object getRoot() {
        return figuresTreeStore.getRootFolder();
    }

    @Override
    public boolean isLeaf(Object node) {
        return ((TreeItem) node).getType() == ItemType.ITEM;
    }

    @Override
    public int getChildCount(Object parent) {
        TreeItem item = (TreeItem) parent;
        List<String> childIds = item.getChildIds();
        if (childIds == null)
            return 0;
        else
            return childIds.size();
    }

    @Override
    public Object getChild(Object parent, int index) {
        String id = ((TreeItem) parent).getChildIds().get(index);
        return figuresTreeStore.read(id);
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return ((TreeItem) parent).getChildIds().indexOf(((TreeItem) child).getId());
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("not yet in use");
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        treeModelSupport.addTreeModelListener(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        treeModelSupport.removeTreeModelListener(l);
    }
}
