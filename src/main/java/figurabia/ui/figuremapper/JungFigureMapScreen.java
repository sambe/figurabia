/*
 * Copyright (c) 2012 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on Aug 1, 2012
 */
package figurabia.ui.figuremapper;

import java.awt.Color;
import java.awt.Paint;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.PickingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.TranslatingGraphMousePlugin;
import figurabia.domain.Figure;
import figurabia.domain.PuertoPosition;
import figurabia.framework.FigureModel;

public class JungFigureMapScreen extends JPanel {

    private static final Color HIGHLIGHT_COLOR = new Color(255, 204, 0);

    private FigureModel figureModel;
    private Graph<PuertoPosition, Integer> graph;
    private Layout<PuertoPosition, Integer> layout;
    private VisualizationViewer<PuertoPosition, Integer> graphPanel;

    public JungFigureMapScreen(FigureModel fm) {
        figureModel = fm;
        graph = new SparseMultigraph<PuertoPosition, Integer>();
        buildGraph();
        Transformer<Integer, Paint> edgePaintTransformer = new Transformer<Integer, Paint>() {
            @Override
            public Paint transform(Integer input) {
                return Color.LIGHT_GRAY;
            }
        };
        Transformer<PuertoPosition, Paint> vertexPaintTransformer = new Transformer<PuertoPosition, Paint>() {
            @Override
            public Paint transform(PuertoPosition input) {
                return Color.LIGHT_GRAY;
            }
        };
        Transformer<PuertoPosition, Paint> vertexFillPaintTransformer = new Transformer<PuertoPosition, Paint>() {
            @Override
            public Paint transform(PuertoPosition input) {
                return Color.DARK_GRAY;
            }
        };
        graphPanel = new VisualizationViewer<PuertoPosition, Integer>(layout);
        RenderContext<PuertoPosition, Integer> rc = graphPanel.getRenderContext();
        rc.setEdgeDrawPaintTransformer(edgePaintTransformer);
        rc.setArrowDrawPaintTransformer(edgePaintTransformer);
        rc.setArrowFillPaintTransformer(edgePaintTransformer);
        rc.setVertexDrawPaintTransformer(vertexPaintTransformer);
        rc.setVertexFillPaintTransformer(vertexFillPaintTransformer);

        PluggableGraphMouse gm = new PluggableGraphMouse();
        gm.add(new TranslatingGraphMousePlugin(MouseEvent.BUTTON3_MASK));
        gm.add(new ScalingGraphMousePlugin(new CrossoverScalingControl(), 0, 0.666666f, 1.5f));
        gm.add(new PickingGraphMousePlugin(MouseEvent.BUTTON1_MASK, 0));
        graphPanel.setGraphMouse(gm);

        setLayout(new MigLayout("ins 0", "[fill]", "[fill]"));
        add(graphPanel, "push");
    }

    public void refreshData() {
        buildGraph();
        graphPanel.setGraphLayout(layout);
        graphPanel.getModel().getRelaxer().setSleepTime(1000);
    }

    private void buildGraph() {
        Collection<Figure> figures = figureModel.getViewSet();
        Map<PuertoPosition, PuertoPosition> nodePositions = new HashMap<PuertoPosition, PuertoPosition>();
        Map<Pair<PuertoPosition>, Integer> edges = new HashMap<Pair<PuertoPosition>, Integer>();
        int edgeNumber = 0;

        // remove all first
        for (Integer e : new ArrayList<Integer>(graph.getEdges())) {
            graph.removeEdge(e);
        }
        for (PuertoPosition pp : new ArrayList<PuertoPosition>(graph.getVertices())) {
            graph.removeVertex(pp);
        }

        // now insert vertices and edges for the figures
        for (Figure f : figures) {
            List<PuertoPosition> positions = f.getPositions();
            // add positions (vertices)
            for (PuertoPosition p : positions) {
                if (nodePositions.containsKey(p))
                    continue;
                graph.addVertex(p);
                nodePositions.put(p, p);
            }
        }
        for (Figure f : figures) {
            List<PuertoPosition> positions = f.getPositions();
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

        //layout = new KKLayout<PuertoPosition, Integer>(graph);
        //layout.setMaxIterations(1000);

        layout = new SpringLayout<PuertoPosition, Integer>(graph);
        layout.setSize(getSize());
    }
}
