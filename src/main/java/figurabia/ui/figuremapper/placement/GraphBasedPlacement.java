/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 18.07.2010
 */
package figurabia.ui.figuremapper.placement;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import figurabia.domain.Element;
import figurabia.domain.Figure;
import figurabia.domain.PuertoPosition;
import figurabia.ui.util.GeometryUtil;
import figurabia.util.Graph;

public class GraphBasedPlacement implements PlacementStrategy {

    private double maxX;
    private double maxY;
    private double margin;
    private Point2D center;
    private double radius;

    public GraphBasedPlacement(double maxX, double maxY, double margin) {
        this.maxX = maxX;
        this.maxY = maxY;
        this.margin = margin;
        this.center = new Point2D.Double(maxX / 2.0, maxY / 2.0);
        this.radius = Math.min(maxX, maxY) * 0.25;
    }

    @Override
    public void assignCoordinates(Collection<Figure> allFigures, Map<PuertoPosition, Point2D> coordinates) {

        Random rand = new Random(0);

        // build a graph data structure
        Graph<PuertoPosition> graph = new Graph<PuertoPosition>(true);
        for (Figure f : allFigures) {
            graph.addAllNodes(f.getPositions());
            for (Element e : f.getElements()) {
                graph.addEdge(e.getInitialPosition(), e.getFinalPosition());
            }
        }

        // save a copy of the original graph
        Graph<PuertoPosition> originalGraph = new Graph<PuertoPosition>(graph);

        // remove lists with trailing parts (create a collection of lists, remembering position to attach it to)
        List<List<PuertoPosition>> trailingParts = new ArrayList<List<PuertoPosition>>();
        for (PuertoPosition position : new HashSet<PuertoPosition>(graph.getNodeSet())) {
            if (graph.getTotalDegree(position) != 1)
                continue;
            List<PuertoPosition> trailingPart = new ArrayList<PuertoPosition>();
            while (graph.getTotalDegree(position) == 1) {
                PuertoPosition newPosition = graph.getAdjacents(position).iterator().next();
                graph.removeEdge(position, newPosition);
                graph.removeNode(position);
                trailingPart.add(position);
                position = newPosition;
            }
            trailingPart.add(position);
            trailingParts.add(trailingPart);
        }

        // remove independent sections (create a collection of lists, remembering both end positions to attach to)
        List<List<PuertoPosition>> indepSections = new ArrayList<List<PuertoPosition>>();
        for (PuertoPosition position : new HashSet<PuertoPosition>(graph.getNodeSet())) {
            if (graph.getTotalDegree(position) != 2)
                continue;
            // move to one end of the independent section (and remove first edge)
            // (caution: beware of loops)
            PuertoPosition previousPosition = position;
            position = graph.getAdjacents(position).iterator().next();
            while (graph.getTotalDegree(position) == 2) {
                for (PuertoPosition p : graph.getAdjacents(position)) {
                    if (!p.equals(previousPosition)) {
                        previousPosition = position;
                        position = p;
                        break;
                    }
                }
            }
            List<PuertoPosition> indepSection = new ArrayList<PuertoPosition>();
            // setting up start position for removal of the independent section
            graph.removeEdge(position, previousPosition);
            indepSection.add(position);
            position = previousPosition;

            // remove all edges of the independent section
            while (graph.getTotalDegree(position) == 1) {
                PuertoPosition newPosition = graph.getAdjacents(position).iterator().next();
                graph.removeEdge(position, newPosition);
                graph.removeNode(position);
                indepSection.add(position);
                position = newPosition;
            }
            indepSection.add(position);
            indepSections.add(indepSection);

            // remove last edge too (has already been removed)
            //graph.removeEdge(indepSection.get(indepSection.size() - 2), position);

            // add a replacement edge for the whole independent section (simplified graph structure)
            graph.addEdge(indepSection.get(0), position, indepSection.size() - 1);
        }

        // devise some clever logic to layout the rest (e.g. arrange on a circle for a start)
        int k = 0;
        int n = graph.getNodeSet().size();
        for (PuertoPosition p : graph.getNodeSet()) {
            double angle = k++ / (double) n * 2.0 * Math.PI;
            coordinates.put(p, GeometryUtil.pointOnCircle(center, radius, angle));
        }

        // reattach sections (backwards, in case of sections depending on other sections)
        for (int i = indepSections.size() - 1; i >= 0; i--) {
            List<PuertoPosition> indepSection = indepSections.get(i);
            Point2D p1 = coordinates.get(indepSection.get(0));
            Point2D p2 = coordinates.get(indepSection.get(indepSection.size() - 1));
            Point2D localCenter = new Point2D.Double((p1.getX() + p2.getX()) / 2.0, (p1.getY() + p2.getY()) / 2.0);
            // for more than one point: arrange them on a half circle
            if (indepSection.size() > 3) {
                double sign = GeometryUtil.dotProduct(p1, center, p2) > 0 ? 1.0 : -1.0;
                double localRadius = p1.distance(p2) / 2.0;
                double baseAngle = GeometryUtil.getAngle(localCenter, p1);
                int m = indepSection.size() - 1;

                // make sure there is a minimal radius for a number of nodes (so that they are not to close to each other)
                double baseOutset = 0.0;
                double angleOffset = 0.0;
                double minRadius = (indepSection.size() - 1) * 20;
                if (localRadius < minRadius) {
                    baseOutset = 0.12 * (minRadius - localRadius) / localRadius;
                    angleOffset = 2.0 * baseOutset * Math.PI / 2.0;
                    localRadius = minRadius;
                }

                // move center of half circle a bit further out
                Point2D diff = GeometryUtil.scaledDiff(center, localCenter, baseOutset + rand.nextDouble() * 0.35);
                localCenter = GeometryUtil.add(localCenter, diff);

                for (int j = 1; j < indepSection.size() - 1; j++) {
                    double angle = j / (double) m * (Math.PI + 2 * angleOffset) * sign + baseAngle - angleOffset * sign;
                    coordinates.put(indepSection.get(j), GeometryUtil.pointOnCircle(localCenter, localRadius, angle));
                }
            } else {
                // otherwise a straight line is enough
                coordinates.put(indepSection.get(1), localCenter);
            }
        }

        // reattach trailing parts
        int t = 0;
        for (int i = trailingParts.size() - 1; i >= 0; i--) {
            List<PuertoPosition> trailingPart = trailingParts.get(i);
            for (int j = trailingPart.size() - 2; j >= 0; j--) {
                coordinates.put(trailingPart.get(j), new Point2D.Double(margin, margin + t++ * 50));
            }
        }
    }
}
