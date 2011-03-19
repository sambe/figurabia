/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 18.07.2010
 */
package figurabia.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Graph<T> {

    private boolean unidirectional;

    private Set<T> nodes = new HashSet<T>();
    private Set<Edge<T>> edges = new HashSet<Edge<T>>();
    private Map<T, Set<T>> in = new HashMap<T, Set<T>>();
    private Map<T, Set<T>> out = new HashMap<T, Set<T>>();

    public Graph() {
        this(false);
    }

    public Graph(boolean unidirectional) {
        this.unidirectional = unidirectional;
    }

    public Graph(Graph<T> graph) {
        this(graph.unidirectional);
        nodes = new HashSet<T>(graph.nodes);
        edges = new HashSet<Edge<T>>(graph.edges);
        in = new HashMap<T, Set<T>>(graph.in);
        out = new HashMap<T, Set<T>>(graph.out);
    }

    public boolean addNode(T node) {
        return nodes.add(node);
    }

    public boolean addAllNodes(Collection<T> allNodes) {
        return nodes.addAll(allNodes);
    }

    public boolean removeNode(T node) {
        boolean contained = nodes.remove(node);
        if (contained) {
            for (T adjacent : _getAdjacentsIn(node)) {
                _removeEdge(new Edge<T>(adjacent, node));
            }
            for (T adjacent : _getAdjacentsOut(node)) {
                _removeEdge(new Edge<T>(node, adjacent));
            }
        }
        return contained;
    }

    public boolean containsNode(T node) {
        return nodes.contains(node);
    }

    public boolean addEdge(T from, T to, double weight) {
        return addEdge(new Edge<T>(from, to));
    }

    public boolean addEdge(T from, T to) {
        return addEdge(new Edge<T>(from, to));
    }

    public boolean addEdge(Edge<T> edge) {
        if (!nodes.contains(edge.from) || !nodes.contains(edge.to)) {
            throw new IllegalStateException("Tried adding an edge to/from a node that is not part of this graph." +
                    " Tried adding: (" + edge.from + ", " + edge.to + ")");
        }
        boolean contained = containsEdge(edge);
        // removing previous edge, to replace it with the new one (because weight could be different)
        if (contained) {
            removeEdge(edge);
        }
        edges.add(edge);
        addToSetEntry(in, edge.to, edge.from);
        addToSetEntry(out, edge.from, edge.to);
        return !contained;
    }

    private void addToSetEntry(Map<T, Set<T>> map, T key, T value) {
        Set<T> set = map.get(key);
        if (set == null) {
            set = new HashSet<T>();
            set.add(value);
            map.put(key, set);
        } else {
            set.add(value);
        }
    }

    public boolean removeEdge(T from, T to) {
        return removeEdge(new Edge<T>(from, to));
    }

    public boolean removeEdge(Edge<T> edge) {
        boolean contained = _removeEdge(edge);
        if (unidirectional) {
            contained = contained || _removeEdge(edge.invert());
        }
        return contained;
    }

    private boolean _removeEdge(Edge<T> edge) {
        boolean contained = edges.remove(edge);
        if (contained) {
            removeFromSetEntry(in, edge.to, edge.from);
            removeFromSetEntry(out, edge.from, edge.to);
        }
        return contained;
    }

    private void removeFromSetEntry(Map<T, Set<T>> map, T key, T value) {
        Set<T> set = map.get(key);
        if (set == null) {
            return;
        } else {
            set.remove(value);
        }
    }

    public boolean containsEdge(T from, T to) {
        return edges.contains(new Edge<T>(from, to));
    }

    public boolean containsEdge(Edge<T> edge) {
        boolean contained = edges.contains(edge);
        if (unidirectional) {
            contained = contained || edges.contains(edge.invert());
        }
        return contained;
    }

    public int getInDegree(T node) {
        if (unidirectional) {
            throw new IllegalStateException("use getTotalDegree for unidirectional graphs");
        }
        return _getInDegree(node);
    }

    private int _getInDegree(T node) {
        Set<T> nodes = in.get(node);
        return nodes == null ? 0 : nodes.size();
    }

    public int getOutDegree(T node) {
        if (unidirectional) {
            throw new IllegalStateException("use getTotalDegree for unidirectional graphs");
        }
        return _getOutDegree(node);
    }

    private int _getOutDegree(T node) {
        Set<T> nodes = out.get(node);
        return nodes == null ? 0 : nodes.size();
    }

    public int getTotalDegree(T node) {
        if (unidirectional) {
            return getAdjacents(node).size();
        }
        return _getInDegree(node) + _getOutDegree(node);
    }

    public Set<T> getAdjacentsIn(T node) {
        if (unidirectional) {
            throw new IllegalStateException("use getAdjacentsTotal for unidirectional graphs");
        }
        return _getAdjacentsIn(node);
    }

    private Set<T> _getAdjacentsIn(T node) {
        Set<T> nodes = in.get(node);
        return nodes == null ? Collections.<T> emptySet() : Collections.unmodifiableSet(nodes);
    }

    public Set<T> getAdjacentsOut(T node) {
        if (unidirectional) {
            throw new IllegalStateException("use getAdjacentsTotal for unidirectional graphs");
        }
        return _getAdjacentsOut(node);
    }

    private Set<T> _getAdjacentsOut(T node) {
        Set<T> nodes = out.get(node);
        return nodes == null ? Collections.<T> emptySet() : Collections.unmodifiableSet(nodes);
    }

    public Set<T> getAdjacents(T node) {
        Set<T> totalNodes = new HashSet<T>();
        totalNodes.addAll(_getAdjacentsIn(node));
        totalNodes.addAll(_getAdjacentsOut(node));
        return totalNodes;
    }

    public Set<T> getNodeSet() {
        return Collections.unmodifiableSet(nodes);
    }

    public Set<Edge<T>> getEdgeSet() {
        return Collections.unmodifiableSet(edges);
    }
}
