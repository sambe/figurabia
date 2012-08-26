/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Aug 5, 2012
 */
package figurabia.ui.figuremapper.placement;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.util.IterativeContext;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import figurabia.domain.Figure;
import figurabia.domain.PuertoPosition;

public class JungPlacementModel<LayoutType extends Layout<PuertoPosition, Integer> & IterativeContext> extends
        AbstractPlacementModel {
    private Graph<PuertoPosition, Integer> graph;
    private LayoutType layout;
    private double layoutSpecificScale;
    //private SpringLayout<PuertoPosition, Integer> layout;
    private Map<PuertoPosition, PuertoPosition> nodePositions = new HashMap<PuertoPosition, PuertoPosition>();
    private Map<Pair<PuertoPosition>, Integer> edges = new HashMap<Pair<PuertoPosition>, Integer>();
    private int edgeNumber = 0;
    private RelaxRunnable relaxRunnable = null;
    private Thread relaxThread = null;
    private int relaxPause = 500;
    private int relaxInitial = 500;
    private Class<LayoutType> layoutType;

    public JungPlacementModel(Class<LayoutType> layoutType, double scale) {
        this.layoutType = layoutType;
        this.layoutSpecificScale = scale;
    }

    @Override
    public Set<PuertoPosition> getAllPositions() {
        return nodePositions.keySet();
    }

    @Override
    public Point2D getCoord(PuertoPosition pp) {
        if (layout == null)
            throw new IllegalStateException("not yet initialized");
        //return GeometryUtil.scale(layout.transform(pp), layoutSpecificScale);
        return layout.transform(pp);
    }

    @Override
    public void setCoord(PuertoPosition pp, Point2D newCoord) {
        //layout.setLocation(pp, GeometryUtil.scale(newCoord, 1.0 / layoutSpecificScale));
        layout.setLocation(pp, newCoord);
    }

    private void addFigureToGraph(Figure f) {
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
            PuertoPosition p1 = nodePositions.get(positions.get(i - 1));
            PuertoPosition p2 = nodePositions.get(positions.get(i));
            Pair<PuertoPosition> pair = new Pair<PuertoPosition>(p1, p2);
            if (!edges.containsKey(pair)) {
                edges.put(pair, edgeNumber);
                graph.addEdge(edgeNumber++, p1, p2, EdgeType.DIRECTED);
            }
        }
    }

    @Override
    public void recalculate(Dimension size) {
        // create a new graph instead of modifying old one to avoid concurrency problems
        // with previous relaxationRunner that might still be running
        graph = new SparseMultigraph<PuertoPosition, Integer>();
        nodePositions = new HashMap<PuertoPosition, PuertoPosition>();
        edges = new HashMap<Pair<PuertoPosition>, Integer>();
        edgeNumber = 0;
        for (Figure f : figures)
            addFigureToGraph(f);
        // recreate layout for changed graph
        //layout = new SpringLayout<PuertoPosition, Integer>(graph);
        try {
            layout = layoutType.getConstructor(Graph.class).newInstance(graph);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        //layout = new KKLayout<PuertoPosition, Integer>(graph);
        layout.setSize(new Dimension((int) (size.width * layoutSpecificScale),
                (int) (size.height * layoutSpecificScale)));
        layout.initialize();
        notifyPlacementChangeListeners();
    }

    @Override
    public void startRelax() {
        if (relaxRunnable != null) {
            stopRelax();
        }
        relaxRunnable = new RelaxRunnable(layout, relaxInitial);
        relaxRunnable.pauseMillis = relaxPause;
        relaxThread = new Thread(relaxRunnable);
        relaxThread.setPriority(Thread.MIN_PRIORITY);
        relaxThread.setDaemon(true);
        relaxThread.start();
    }

    @Override
    public void stopRelax() {
        if (relaxRunnable != null) {
            relaxRunnable.pauseMillis = -1;
            relaxRunnable = null;
            try {
                relaxThread.interrupt();
                relaxThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            relaxThread = null;
        }
    }

    @Override
    public void setPause(int millis) {
        relaxPause = millis;
        if (relaxRunnable != null) {
            relaxRunnable.pauseMillis = millis;
        }
    }

    @Override
    public void setInitial(int millis) {
        relaxInitial = millis;
        if (relaxRunnable != null) {
            relaxRunnable.initialMillis = millis;
        }
    }

    public class RelaxRunnable implements Runnable {
        private volatile int pauseMillis;
        private int initialMillis;
        private final IterativeContext layout;

        public RelaxRunnable(IterativeContext layout, int initialMillis) {
            this.layout = layout;
            this.initialMillis = initialMillis;
        }

        @Override
        public void run() {
            long startMillis = System.currentTimeMillis();
            int millis;
            // do some initial relaxation steps
            while (!layout.done() && System.currentTimeMillis() < startMillis + initialMillis) {
                layout.step();
            }
            // then follow with the regular steps in equal time intervals
            while ((millis = pauseMillis) != -1 && !layout.done()) {
                layout.step();
                if (pauseMillis == -1)
                    break;
                // doing a synchronous call to avoid concurrent access to layout
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        notifyPlacementChangeListeners();
                    }
                });
                try {
                    Thread.sleep(millis);
                } catch (InterruptedException e) {
                    // just terminate
                    break;
                }
            }
        }
    }
}
