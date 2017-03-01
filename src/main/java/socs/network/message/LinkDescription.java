package socs.network.message;


import socs.network.node.Router;
import socs.network.node.RouterDescription;

import java.io.Serializable;
import java.util.Objects;

public class LinkDescription implements Serializable {
  private String linkID;
  private int portNum;
  private int tosMetrics;


  public LinkDescription(String linkID, int portNum, int tosMetrics) {
    this.linkID = linkID;
    this.portNum = portNum;
    this.tosMetrics = tosMetrics;
  }

  public LinkDescription(LinkDescriptionBuilder b) {
    this.linkID = b.linkID;
    this.portNum = b.portNum;
    this.tosMetrics = b.tosMetrics;
  }


  public String getLinkID() {
    return linkID;
  }


  @Override
  public boolean equals(Object obj) {
    if (Objects.isNull(obj) || !(obj instanceof LinkDescription))
      return false;
    LinkDescription lhs = (LinkDescription) obj;
    return (lhs.tosMetrics == tosMetrics && lhs.portNum == portNum && lhs.linkID == linkID);
  }

  public int getPortNum() {
    return portNum;
  }

  public int getTosMetrics() {
    return tosMetrics;
  }

  public String toString() {
    return linkID + ","  + portNum + "," + tosMetrics;
  }


  public static class LinkDescriptionBuilder {

    private String linkID;
    private int portNum;
    private int tosMetrics;


    public LinkDescriptionBuilder linkID(String linkID) {
      this.linkID = linkID;
      return this;
    }

    public LinkDescriptionBuilder portNum(int portNum) {
      this.portNum = portNum;
      return this;
    }

    public  LinkDescriptionBuilder tosMetrics(int weight) {
      this.tosMetrics = weight;
      return this;
    }

    public LinkDescription build() {
      return new LinkDescription(this);
    }


  }

}
