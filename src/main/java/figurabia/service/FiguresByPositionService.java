/*
 * Copyright (c) 2009 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 31.12.2009
 */
package figurabia.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import figurabia.domain.Element;
import figurabia.domain.Figure;
import figurabia.domain.PuertoPosition;
import figurabia.framework.FigurabiaModel;

public class FiguresByPositionService {
    private FigurabiaModel figurabiaModel;
    private Map<PuertoPosition, Map<Element, List<Result>>> figuresByPosition;

    public FiguresByPositionService(final FigurabiaModel figurabiaModel) {
        this.figurabiaModel = figurabiaModel;
        init(figurabiaModel.getViewSet());
    }

    /**
     * Creates the index of all the figures.
     * 
     * @param figures the figures that should be retrievable afterwards
     */
    public void init(Collection<Figure> figures) {
        figuresByPosition = new HashMap<PuertoPosition, Map<Element, List<Result>>>();
        for (Figure f : figures) {
            List<PuertoPosition> positions = f.getPositions();
            List<Element> elements = f.getElements();
            for (int i = 0; i < positions.size() - 1; i++) {
                PuertoPosition p = positions.get(i);
                Map<Element, List<Result>> resultMap = figuresByPosition.get(p);
                if (resultMap == null) {
                    resultMap = new HashMap<Element, List<Result>>();
                    figuresByPosition.put(p, resultMap);
                }
                Element e = elements.get(i);
                List<Result> resultList = resultMap.get(e);
                if (resultList == null) {
                    resultList = new ArrayList<Result>();
                    resultMap.put(e, resultList);
                }
                resultList.add(new Result(f, i));
            }
        }

        // make all collections immutable
        for (PuertoPosition p : figuresByPosition.keySet()) {
            Map<Element, List<Result>> resultMap = figuresByPosition.get(p);
            for (Element e : resultMap.keySet()) {
                resultMap.put(e, Collections.unmodifiableList(resultMap.get(e)));
            }
            figuresByPosition.put(p, Collections.unmodifiableMap(resultMap));
        }
    }

    public static class Result {
        public final Figure figure;
        public final int index;

        public Result(Figure figure, int index) {
            this.figure = figure;
            this.index = index;
        }

        public Element getElement() {
            return figure.getElements().get(index);
        }
    }

    public Map<Element, List<Result>> retrieveFiguresByPosition(PuertoPosition position) {
        Map<Element, List<Result>> resultMap = figuresByPosition.get(position);
        if (resultMap == null)
            return Collections.emptyMap();
        return resultMap;
    }
}
