package socs.network.util.ds;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by ericschaal on 2017-03-01.
 * Builds a standard Graph from a collection of LSA
 */
public class Graph {

    private List<Vertex> vertices;
    private List<Edge> edges;


    public Graph(Collection<LSA> lsas) {

        vertices = new LinkedList<>();
        edges = new LinkedList<>();

        // adding vertices
        for (LSA lsa : lsas) {
            for (LinkDescription link : lsa.links) {
                Vertex vertex = new Vertex(link.getLinkID());
                if (!vertices.contains(vertex))
                    vertices.add(vertex);
            }
        }

        // adding edges
        for (LSA lsa : lsas) {
            for (LinkDescription link : lsa.links) {
                Vertex source = getVertexWith(lsa.linkStateID);
                Vertex destination = getVertexWith(link.getLinkID());
                Edge edge = new Edge(source, destination, link.getTosMetrics());
                if (!edges.contains(edge))
                    edges.add(edge);
            }
        }


    }


    /**
     * Returns vertex with ID id
     * @param id
     * @return vertex
     */
    public Vertex getVertexWith(String id) {
        for (Vertex v : vertices) {
            if (v.getId().equals(id))
                return v;
        }
        return null;
    }


    // getters

    public List<Vertex> getVertices() {
        return vertices;
    }

    public List<Edge> getEdges() {
        return edges;
    }
}
