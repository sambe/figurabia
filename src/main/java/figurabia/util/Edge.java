/*
 * Copyright (c) 2010 by Samuel Berner (samuel.berner@gmail.com), all rights reserved
 * Created on 18.07.2010
 */
package figurabia.util;

public final class Edge<T> {
    public final T from;
    public final T to;
    public final double weight;

    public Edge(T from, T to, double weight) {
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    public Edge(T from, T to) {
        this(from, to, 1.0);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Edge) {
            Edge e = (Edge) obj;
            return from.equals(e.from) && to.equals(e.to);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return from.hashCode() + 31 * to.hashCode();
    }

    public Edge<T> invert() {
        return new Edge<T>(to, from);
    }
}
