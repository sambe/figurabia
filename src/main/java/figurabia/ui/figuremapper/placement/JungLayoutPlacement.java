/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 09.04.2012
 */
package figurabia.ui.figuremapper.placement;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import figurabia.domain.Figure;
import figurabia.domain.PuertoPosition;

public class JungLayoutPlacement implements PlacementStrategy {

    private Dimension size;

    public JungLayoutPlacement(Dimension size) {
        this.size = new Dimension(size.width * 5, size.height * 5);
    }

    @Override
    public void assignCoordinates(Collection<Figure> allFigures, Map<PuertoPosition, Point2D> coordinates) {
        Graph<PuertoPosition, Integer> graph = new SparseMultigraph<PuertoPosition, Integer>();
        Map<PuertoPosition, PuertoPosition> nodePositions = new HashMap<PuertoPosition, PuertoPosition>();
        int edgeNumber = 0;

        for (Figure f : allFigures) {
            List<PuertoPosition> positions = f.getPositions();
            // add positions (vertices)
            for (PuertoPosition p : positions) {
                if (nodePositions.containsKey(p))
                    continue;
                graph.addVertex(p);
                nodePositions.put(p, p);
            }

            // add edges
            for (int i = 1; i < positions.size(); i++) {
                PuertoPosition p1 = positions.get(i - 1);
                if (nodePositions.containsKey(p1)) {
                    p1 = nodePositions.get(p1);
                }
                PuertoPosition p2 = positions.get(i);
                if (nodePositions.containsKey(p2)) {
                    p2 = nodePositions.get(p2);
                }
                graph.addEdge(edgeNumber++, p1, p2, EdgeType.DIRECTED);
            }
        }

        KKLayout<PuertoPosition, Integer> layout = new KKLayout<PuertoPosition, Integer>(graph);
        layout.setSize(size);

        for (int i = 0; !layout.done() && i < 1000; i++) {
            layout.step();
        }

        // set coordinates for all positions
        for (PuertoPosition p : graph.getVertices()) {
            double x = layout.getX(p);
            double y = layout.getY(p);
            coordinates.put(p, new Point2D.Double(x - size.width / 2, y - size.height / 2));
        }
    }

}
