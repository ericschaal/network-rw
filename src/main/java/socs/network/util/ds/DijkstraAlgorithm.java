package socs.network.util.ds;

import java.util.*;

/**
 * Created by ericschaal on 2017-03-01.
 */
public class DijkstraAlgorithm {

    private final List<Vertex> vertices;
    private final List<Edge> edges;
    private Set<Vertex> visited;
    private Set<Vertex> unvisited;
    private Map<Vertex, Vertex> predecessor;
    private Map<Vertex, Integer> distance;
    private Vertex start;

    public DijkstraAlgorithm( Graph graph) {
        this.vertices = new ArrayList(graph.getVertices());
        this.edges = new ArrayList(graph.getEdges());
    }

    public void start(Vertex source) {
        visited = new HashSet();
        unvisited = new HashSet();
        distance = new HashMap();
        predecessor = new HashMap();
        start = source;
        distance.put(source, 0);
        unvisited.add(source);
        while (unvisited.size() > 0) {
            Vertex node = getMinimum(unvisited);
            visited.add(node);
            unvisited.remove(node);
            findMinimalDistances(node);
        }
    }

    /**
     * Returns the weight of link src->dst or -1 if not found
     * @param src Source vertex
     * @param dst Destination vertex
     * @return weight of the link
     */
    private int getWeight(Vertex src, Vertex dst) {
        for (Edge edge : edges) {
            if (edge.getSource().equals(src) && edge.getDestination().equals(dst))
                return edge.getWeight();
        }
        return -1;
    }

    /**
     * Compare current distance and update distance
     * @param node
     */
    private void findMinimalDistances(Vertex node) {
        List<Vertex> adjacent = getNeighbors(node);
        for (Vertex target : adjacent) {
            if (getShortestDistance(target) > getShortestDistance(node)
                    + getWeight(node, target)) { //
                distance.put(target, getShortestDistance(node)
                        + getWeight(node, target)); //
                predecessor.put(target, node);
                unvisited.add(target);
            }
        }

    }


    /**
     * Returns neighbors of vertex
     * @param vertex
     * @return list of all neighbors
     */
    private List<Vertex> getNeighbors(Vertex vertex) {
        List<Vertex> neighbors = new ArrayList<Vertex>();
        for (Edge edge : edges) {
            if (edge.getSource().equals(vertex)
                    && !isVisited(edge.getDestination())) {
                neighbors.add(edge.getDestination());
            }
        }
        return neighbors;
    }

    /**
     * Fail proof min function
     * @param vertices
     * @return
     */
    private Vertex getMinimum(Set<Vertex> vertices) {
        Vertex minimum = null;
        for (Vertex vertex : vertices) {
            if (minimum == null) {
                minimum = vertex;
            } else {
                if (getShortestDistance(vertex) < getShortestDistance(minimum)) {
                    minimum = vertex;
                }
            }
        }
        return minimum;
    }

    private boolean isVisited(Vertex vertex) {
        return visited.contains(vertex);
    }

    private int getShortestDistance(Vertex destination) {
        Integer d = distance.get(destination);
        if (d == null) {
            return Integer.MAX_VALUE;
        } else {
            return d;
        }
    }


    /**
     * Returns a path in the form of a list of edges.
     * @param target destination
     * @return
     */
    public LinkedList<Edge> getPathWithDistance(Vertex target) {
        LinkedList<Edge> path = new LinkedList();
        Vertex step = target;

        // check if a path exists
        if (Objects.isNull(target) || Objects.isNull(start))
            throw new IllegalArgumentException();

        // loop
        if (target.equals(start)) {
            path.add(new Edge(start, start, 0));
            return path;
        }

        if (predecessor.get(step) == null) {
            return null;
        }
        path.add(new Edge(predecessor.get(step), step, getWeight(predecessor.get(step), step)));
        while (predecessor.get(predecessor.get(step)) != null) {
            step = predecessor.get(step);
            path.add(new Edge(predecessor.get(step), step, getWeight(predecessor.get(step), step)));
        }
        // Put it into the correct order
        Collections.reverse(path);
        return path;
    }


}
