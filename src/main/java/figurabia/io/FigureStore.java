/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Jul 1, 2012
 */
package figurabia.io;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.thoughtworks.xstream.XStream;

import figurabia.domain.Element;
import figurabia.domain.Figure;
import figurabia.domain.PuertoOffset;
import figurabia.domain.PuertoPosition;
import figurabia.io.store.StoreListener;
import figurabia.io.store.XStreamStore;
import figurabia.io.workspace.Workspace;

public class FigureStore extends XStreamStore<Figure> {

    private Set<Figure> activeFigures = new HashSet<Figure>();

    public FigureStore(Workspace workspace, String basePath) {
        super(createXStream(), workspace, basePath, Figure.class);

        // init active figures
        for (Figure f : allObjects()) {
            if (f.isActive()) {
                activeFigures.add(f);
            }
        }
        // add listener to stay up to date
        addStoreListener(new StoreListener<Figure>() {
            @Override
            public void update(figurabia.io.store.StoreListener.StateChange change, Figure f) {
                switch (change) {
                case CREATED:
                case UPDATED:
                    if (f.isActive())
                        activeFigures.add(f);
                    else
                        activeFigures.remove(f);
                    break;
                case DELETED:
                    activeFigures.remove(f);
                    break;
                }
            }
        });

        validate();
    }

    private static XStream createXStream() {
        XStream xstream = new XStream();
        xstream.alias("Figure", Figure.class);
        xstream.alias("PuertoPosition", PuertoPosition.class);
        xstream.alias("PuertoOffset", PuertoOffset.class);
        xstream.alias("Element", Element.class);
        xstream.omitField(Element.class, "initialPosition");
        xstream.omitField(Element.class, "finalPosition");
        xstream.omitField(Figure.class, "parent");
        return xstream;
    }

    public Collection<Figure> getAllFigures() {
        return Collections.unmodifiableCollection(allObjects());
    }

    public Collection<Figure> getAllActiveFigures() {
        return Collections.unmodifiableCollection(activeFigures);
    }

    private void validate() {
        for (Figure f : allObjects()) {
            int positions = f.getPositions().size();
            if (f.getVideoPositions() == null)
                throw new IllegalStateException("figure " + f.toString() + " has no video positions (null)");
            if (f.getVideoPositions().size() != positions) {
                throw new IllegalStateException("figure " + f.toString() + " has " + f.getVideoPositions().size()
                        + " video positions when it should have " + positions);
            }
            if (f.getElements() == null)
                throw new IllegalStateException("figure " + f.toString() + " has no elements (null)");
            if (f.getElements().size() != 0 && f.getElements().size() != positions - 1)
                throw new IllegalStateException("figure " + f.toString() + " has " + f.getElements().size()
                        + " elements when it should have " + (positions - 1));
            if (f.getBarIds() == null)
                throw new IllegalStateException("figure " + f.toString() + " has no barIds (null)");
            if (f.getBarIds().size() != positions)
                throw new IllegalStateException("figure " + f.toString() + " has " + f.getBarIds().size()
                        + " barIds when it should have " + positions);

            //TODO possible validations: for active figures: 1-5 alternating beats & barIds regular 11223344...etc.
        }
    }

    @Override
    protected void postLoad(Figure f) {
        List<Element> elements = f.getElements();
        List<PuertoPosition> positions = f.getPositions();
        if (elements != null && positions != null) {
            for (int i = 0; i < elements.size(); i++) {
                Element e = elements.get(i);
                e.setInitialPosition(positions.get(i));
                e.setFinalPosition(positions.get(i + 1));
            }
        }
    }
}
