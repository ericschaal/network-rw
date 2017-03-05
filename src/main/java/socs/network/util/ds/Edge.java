package socs.network.util.ds;

import java.util.Objects;

/**
 * Created by ericschaal on 2017-03-01.
 */
public class Edge  {
    private final Vertex source;
    private final Vertex destination;
    private final int weight;

    public Edge(Vertex source, Vertex destination, int weight) {
        this.source = source;
        this.destination = destination;
        this.weight = weight;
    }

    @Override
    public boolean equals(Object obj) {
        if (Objects.isNull(obj) || !(obj instanceof Edge))
            return false;
        Edge lhs = (Edge) obj;
        return  (lhs.destination.equals(destination) && source.equals(lhs.source));
    }

    public Vertex getDestination() {
        return destination;
    }

    public Vertex getSource() {
        return source;
    }
    public int getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return source + "->" + destination;
    }


}