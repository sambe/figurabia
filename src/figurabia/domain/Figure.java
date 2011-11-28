/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 20.07.2009
 */
package figurabia.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Figure implements Serializable, Cloneable, FolderItem {

    private final static long serialVersionUID = -2708230799923225277L;

    /**
     * Id of the figure (set when it is made persistent for the first time)
     */
    private int id;
    private Folder parent;
    /**
     * Name of this figure (optional)
     */
    private String name;
    private String videoName;
    /**
     * The video times where the positions start (in nanoseconds).
     */
    private List<Long> videoPositions;
    /**
     * All positions, including the last one (stopping position)
     */
    private List<PuertoPosition> positions;
    /**
     * All elements leading from one position to the next.
     */
    private List<Element> elements;
    /**
     * The basic offset for the figure (initial situation).
     */
    private PuertoOffset baseOffset;
    /**
     * If the figure is active (only active figures are shown in the explorer)
     */
    private boolean active;
    /**
     * A list of bar IDs (only used when inactive, to keep the images linked).
     */
    private List<Integer> barIds;

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
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

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the videoName
     */
    public String getVideoName() {
        return videoName;
    }

    /**
     * @param videoName the videoName to set
     */
    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    /**
     * @return the videoPositions
     */
    public List<Long> getVideoPositions() {
        return videoPositions;
    }

    /**
     * @param videoPositions the videoPositions to set
     */
    public void setVideoPositions(List<Long> videoPositions) {
        this.videoPositions = videoPositions;
    }

    /**
     * @return the positions
     */
    public List<PuertoPosition> getPositions() {
        return positions;
    }

    /**
     * @param positions the positions to set
     */
    public void setPositions(List<PuertoPosition> positions) {
        this.positions = positions;
    }

    /**
     * @return the elements
     */
    public List<Element> getElements() {
        return elements;
    }

    /**
     * @param elements the elements to set
     */
    public void setElements(List<Element> elements) {
        this.elements = elements;
    }

    /**
     * @return the baseOffset
     */
    public PuertoOffset getBaseOffset() {
        return baseOffset;
    }

    /**
     * @param baseOffset the baseOffset to set
     */
    public void setBaseOffset(PuertoOffset baseOffset) {
        this.baseOffset = baseOffset;
    }

    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @return the barIds
     */
    public List<Integer> getBarIds() {
        return barIds;
    }

    /**
     * @param barIds the barIds to set
     */
    public void setBarIds(List<Integer> barIds) {
        this.barIds = barIds;
    }

    public PuertoOffset getCombinedOffset(int index) {
        PuertoOffset p = getBaseOffset();
        for (int i = 0; i < index; i++) {
            p = p.addOffset(getElements().get(i).getOffsetChange());
        }
        return p;
    }

    @Override
    public String toString() {
        StringBuilder nameBuf = new StringBuilder();
        if (name == null)
            nameBuf.append(videoName);
        else
            nameBuf.append(name);

        // add active state
        if (!active) {
            nameBuf.append(" (inactive)");
        }
        return nameBuf.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Figure) {
            Figure f = (Figure) obj;
            if (id == f.id) {
                if (this != f)
                    throw new IllegalStateException("Found two different figure objects with id " + id);
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public Figure clone() {
        try {
            Figure clone = (Figure) super.clone();
            if (videoPositions != null) {
                clone.videoPositions = new ArrayList<Long>(videoPositions);
            }
            if (positions != null) {
                clone.positions = new ArrayList<PuertoPosition>(positions);
            }
            if (elements != null) {
                clone.elements = new ArrayList<Element>(elements.size());
                for (Element e : elements) {
                    clone.elements.add(e.clone());
                }
            }
            if (barIds != null) {
                clone.barIds = new ArrayList<Integer>(barIds);
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}
