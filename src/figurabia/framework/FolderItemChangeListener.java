/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 27.11.2011
 */
package figurabia.framework;

import figurabia.domain.Folder;
import figurabia.domain.FolderItem;

public interface FolderItemChangeListener {
    public void itemAdded(Folder parent, int index, FolderItem item);

    public void itemChanged(FolderItem item);

    public void itemRemoved(Folder parent, int index, FolderItem item);

    public void newRootFolder();
}
