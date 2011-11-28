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

import figurabia.domain.Folder;
import figurabia.domain.FolderItem;
import figurabia.framework.FolderItemChangeListener;
import figurabia.framework.PersistenceProvider;

public class FiguresTreeModel implements TreeModel {

    private final TreeModelSupport treeModelSupport = new TreeModelSupport(this);
    private final PersistenceProvider persistenceProvider;

    public FiguresTreeModel(PersistenceProvider pp) {
        this.persistenceProvider = pp;

        persistenceProvider.addFolderItemChangeListener(new FolderItemChangeListener() {
            @Override
            public void newRootFolder() {
                treeModelSupport.fireNewRoot();
            }

            @Override
            public void itemRemoved(Folder parent, int index, FolderItem item) {
                TreePath treePath = createTreePath(parent);
                treeModelSupport.fireChildRemoved(treePath, index, item);
            }

            @Override
            public void itemChanged(FolderItem item) {
                TreePath path = createTreePath(item);
                treeModelSupport.firePathChanged(path);
            }

            @Override
            public void itemAdded(Folder parent, int index, FolderItem item) {
                TreePath parentPath = createTreePath(parent);
                treeModelSupport.fireChildAdded(parentPath, index, item);
            }
        });
    }

    private Object[] createTreePathArray(FolderItem item, int n) {
        if (item == null)
            return new Object[n];
        Object[] array = createTreePathArray(item.getParent(), n + 1);
        array[array.length - 1 - n] = item;
        return array;
    }

    public TreePath createTreePath(FolderItem item) {
        return new TreePath(createTreePathArray(item, 0));
    }

    @Override
    public Object getRoot() {
        return persistenceProvider.getRootFolder();
    }

    @Override
    public boolean isLeaf(Object node) {
        return !(node instanceof Folder);
    }

    @Override
    public int getChildCount(Object parent) {
        if (parent instanceof Folder) {
            List<FolderItem> items = persistenceProvider.getItems((Folder) parent);
            return items == null ? 0 : items.size();
        }
        return 0;
    }

    @Override
    public Object getChild(Object parent, int index) {
        return persistenceProvider.getItems((Folder) parent).get(index);
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        return persistenceProvider.getItems((Folder) parent).indexOf(child);
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
