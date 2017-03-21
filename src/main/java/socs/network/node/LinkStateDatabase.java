package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.util.ds.DijkstraAlgorithm;
import socs.network.util.ds.Edge;
import socs.network.util.ds.Graph;
import socs.network.util.ds.Vertex;
import socs.network.util.error.NoPath;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

public class LinkStateDatabase {

  //linkStateID => LSAInstance
  private HashMap<String, LSA> _store = new HashMap<String, LSA>();

  private RouterDescription rd = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    rd = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.linkStateID, l);
  }

  /**
   * output the shortest path from this router to the destination with the given IP address
   */
  public String getShortestPath(String destinationIP) throws NoPath {

    Graph graph = new Graph(getAllLSA());
    LinkedList<Edge> path;

    DijkstraAlgorithm alg = new DijkstraAlgorithm(graph);

    Vertex source = graph.getVertexWith(rd.getSimulatedIPAddress());
    Vertex destination = graph.getVertexWith(destinationIP);

    if (Objects.isNull(source))
      throw new RuntimeException("Couldn't find source vertex."); // should never happen

    if (Objects.isNull(destination)) // cant find destination
      throw new NoPath();

    alg.start(source);
    path = alg.getPathWithDistance(destination);

    StringBuilder sb = new StringBuilder();


    if (Objects.isNull(path)) // cant find path
      throw new NoPath();


    // building string in correct format
    sb.append(path.getFirst().getSource() + "->");
    for (Edge s : path) {
      sb.append("(" + s.getWeight() + ")");
      sb.append(s.getDestination());
      if (!path.getLast().getDestination().equals(s.getDestination())) {
        sb.append("->");
      }
    }

    return sb.toString();

  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
    lsa.linkStateID = rd.getSimulatedIPAddress();
    lsa.lsaSeqNumber = Integer.MIN_VALUE;
    LinkDescription ld = new LinkDescription.LinkDescriptionBuilder()
            .linkID(rd.getSimulatedIPAddress())
            .portNum(-1)
            .tosMetrics(0)
            .build();
    lsa.links.add(ld);
    return lsa;
  }

  public synchronized void addToStore(String linkID, LSA instance) {
    _store.put(linkID,instance);
  }

  public synchronized Collection<LSA> getAllLSA() {
    return _store.values();
  }

  public synchronized boolean removeFromStore(String source, LinkDescription link) {

    boolean result = _store.get(source).links.remove(link);
    if (result)
      _store.get(source).lsaSeqNumber++;
    return result;
  }

  public synchronized LSA getFromStore(String linkstateID) {
    return _store.get(linkstateID);
  }


  public synchronized String toString() {
    StringBuilder sb = new StringBuilder();
    for (LSA lsa: _store.values()) {
      sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
      for (LinkDescription ld : lsa.links) {
        sb.append(ld.getLinkID()).append(",").append(ld.getPortNum()).append(",").
                append(ld.getTosMetrics()).append("\t");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

}
