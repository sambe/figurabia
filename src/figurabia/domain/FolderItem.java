/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 13.11.2011
 */
package figurabia.domain;

public interface FolderItem {

    String getName();

    Folder getParent();

    void setParent(Folder parent);
}
