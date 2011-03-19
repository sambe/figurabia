/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 19.07.2009
 */
package figurabia.framework;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import figurabia.domain.Element;
import figurabia.domain.Figure;
import figurabia.domain.PuertoPosition;

/**
 * A persistence provider provides all functionality that is necessary for obtaining and persisting domain data. This
 * interface enables the application to stay independent of any specific persistence API.
 * 
 * @author Samuel Berner
 */
public interface PersistenceProvider {

    /**
     * Returns the figure with the given ID.
     * 
     * @param id the ID
     * @return the figure
     */
    Figure getFigureById(int id);

    /**
     * Returns an unmodifiable collection of all figures.
     * 
     * @return the collection of all figures
     */
    Collection<Figure> getAllFigures();

    /**
     * Returns an unmodifiable collection of all active figures.
     * 
     * @return the collection of all active figures
     */
    Collection<Figure> getAllActiveFigures();

    Collection<Element> getAllElements();

    /**
     * Returns an unmodifiable map of all named positions.
     * 
     * @return the map
     */
    Map<String, PuertoPosition> getNamedPositions();

    /**
     * Adds a position to the map of all named positions.
     * 
     * @param name the name of the position
     * @param position the position
     */
    void addNamedPosition(String name, PuertoPosition position);

    /**
     * Deletes a position of the map of all named positions.
     * 
     * @param name the name of the position to delete
     */
    void deleteNamedPosition(String name);

    /**
     * Persists a figure which has no ID yet (was not persisted before).
     * 
     * @param figure the figure to persist
     * @return the ID under which the figure was persisted
     */
    int persistFigure(Figure figure);

    /**
     * Updates an already persisted figure (overwrites the existing figure with the same ID with the given one).
     * 
     * @param figure the figure to update
     */
    void updateFigure(Figure figure);

    /**
     * Clones the figure (creates a new figure with the same data, but a new ID)
     * 
     * @param figure the figure to clone (is left unmodified)
     */
    Figure cloneFigure(Figure figure);

    /**
     * Deletes a previously persisted figure.
     * 
     * @param figure the figure to delete
     */
    void deleteFigure(Figure figure);

    void persistElement(Element element);

    void updateElement(Element element);

    void deleteElement(Element element);

    /**
     * Opens and initializes the persistence provider. It is mandatory to call this method before calling any of the
     * persistence-related methods.
     * 
     * @throws IOException in case of IO problems (e.g. file not found)
     */
    void open() throws IOException;

    /**
     * Writes the changes to a persistent storage and closes the persistence provider. It is not allowed to call any of
     * the persistence-related methods after calling this method.
     * 
     * @throws IOException in case of IO problems (e.g. no write privileges)
     */
    void close() throws IOException;

    /**
     * Writes all changes to a persistent storage (call this whenever the user has completed something).
     * 
     * @throws IOException in case of IO problems (e.g. no write privileges)
     */
    void sync() throws IOException;

    /**
     * Adds a figure change listener which allows to listen for persisting, updating and deleting of figures.
     * 
     * @param listener
     */
    void addFigureChangeListener(FigureListener listener);

    /**
     * Removes a figure change listener.
     * 
     * @param listener
     */
    void removeFigureChangeListener(FigureListener listener);
}
