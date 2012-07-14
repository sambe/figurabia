/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 1, 2012
 */
package figurabia.domain;

import java.util.ArrayList;
import java.util.List;

import figurabia.io.store.Identifiable;

public class TreeItem implements Identifiable {

    public enum ItemType {
        FOLDER, ITEM
    };

    private String id;
    private String rev;
    private ItemType type;
    private String name;
    private String refId;
    private List<String> childIds = new ArrayList<String>();

    public TreeItem() {
    }

    public TreeItem(String id, ItemType type, String name) {
        this.id = id;
        this.rev = "0";
        this.type = type;
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getRev() {
        return rev;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void setRev(String rev) {
        this.rev = rev;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRefId() {
        return refId;
    }

    public void setRefId(String refId) {
        this.refId = refId;
    }

    public List<String> getChildIds() {
        return childIds;
    }

    @Override
    public String toString() {
        return name;
    }
}
