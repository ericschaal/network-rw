package socs.network.util.ds;

/**
 * Created by ericschaal on 2017-03-01.
 */
public class Step {

    public Vertex v;
    public int dist;

    public Step(Vertex v, int dist) {
        this.v = v;
        this.dist = dist;
    }
}
