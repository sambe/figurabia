/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 20.07.2009
 */
package figurabia.domain;

import java.io.Serializable;

public class Element implements Serializable, Cloneable {

    private static final long serialVersionUID = 8461802880795105159L;

    private PuertoPosition initialPosition;
    private PuertoPosition finalPosition;
    private PuertoOffset offsetChange;
    private String name;

    /**
     * @return the intialPosition
     */
    public PuertoPosition getInitialPosition() {
        return initialPosition;
    }

    /**
     * @param initialPosition the intialPosition to set
     */
    public void setInitialPosition(PuertoPosition initialPosition) {
        this.initialPosition = initialPosition;
    }

    /**
     * @return the finalPosition
     */
    public PuertoPosition getFinalPosition() {
        return finalPosition;
    }

    /**
     * @param finalPosition the finalPosition to set
     */
    public void setFinalPosition(PuertoPosition finalPosition) {
        this.finalPosition = finalPosition;
    }

    /**
     * @return the offsetChange
     */
    public PuertoOffset getOffsetChange() {
        return offsetChange;
    }

    /**
     * @param offsetChange the offsetChange to set
     */
    public void setOffsetChange(PuertoOffset offsetChange) {
        this.offsetChange = offsetChange;
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Element) {
            Element el = (Element) obj;
            // maybe consider name too
            return initialPosition.equals(el.initialPosition) && finalPosition.equals(el.finalPosition)
                    && offsetChange.equals(el.offsetChange);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return initialPosition.hashCode() + 13 * (finalPosition.hashCode() + 13 * offsetChange.hashCode());
    }

    @Override
    public Element clone() {
        try {
            Element clone = (Element) super.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException(e);
        }
    }
}
