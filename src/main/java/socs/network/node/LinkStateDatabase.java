package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.util.ds.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

public class LinkStateDatabase {

  //linkStateID => LSAInstance
  HashMap<String, LSA> _store = new HashMap<String, LSA>();

  private RouterDescription rd = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    rd = routerDescription;
    LSA l = initLinkStateDatabase();
    _store.put(l.linkStateID, l);
  }

  /**
   * output the shortest path from this router to the destination with the given IP address
   */
  public String getShortestPath(String destinationIP) {

    Graph graph = new Graph(getAllLSA());
    LinkedList<Edge> path;

    DijkstraAlgorithm alg = new DijkstraAlgorithm(graph);
    alg.execute(graph.getVertexWith(rd.getSimulatedIPAddress()));
    path = alg.getPathWithDistance(graph.getVertexWith(destinationIP));

    StringBuilder sb = new StringBuilder();

    for (Vertex v : graph.getVertices()) {
      System.out.println(v.getId());
    }

    for (Edge e : graph.getEdges()) {
      System.out.println(e.getSource() + "->" + e.getDestination() + "(" + e.getWeight() + ")");
    }

    System.out.println();
    System.out.println();

    if (Objects.isNull(path))
      throw new IllegalStateException();

    sb.append(path.getFirst().getSource() + "->");
    for (Edge s : path) {
      //if (!path.getFirst().v.equals(s.v))
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

  public void addToStore(String linkID, LSA instance) {
    _store.put(linkID,instance);
  }

  public Collection<LSA> getAllLSA() {
    return _store.values();
  }

  public LSA getFromStore(String linkstateID) {
    return _store.get(linkstateID);
  }


  public String toString() {
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
