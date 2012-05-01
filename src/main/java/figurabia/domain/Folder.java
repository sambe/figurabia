/*
 * Copyright (c) 2011 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 13.11.2011
 */
package figurabia.domain;

public class Folder implements FolderItem {

    private int id;
    private String name;
    private Folder parent;

    public Folder(int id, String name, Folder parent) {
        this.id = id;
        this.name = name;
        this.parent = parent;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the name
     */
    @Override
    public String getName() {
        return null;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the parent
     */
    public Folder getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(Folder parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return name;
    }
}
