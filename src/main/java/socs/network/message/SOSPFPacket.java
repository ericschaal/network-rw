package socs.network.message;

import java.io.Serializable;
import java.util.Vector;

public class SOSPFPacket implements Serializable {

    static final long serialVersionUID = 42L;


    //for inter-process communication
    public String srcProcessIP;
    public short srcProcessPort;

    //simulated IP address
    public String srcIP;
    public String dstIP;

    //common header
    public short sospfType; //0 - HELLO, 1 - LinkState Update
    public String routerID;

    //used by HELLO message to identify the sender of the message
    //e.g. when router A sends HELLO to its neighbor, it has to fill this field with its own
    //simulated IP address
    public String neighborID; //neighbor's simulated IP address

    //used by LSAUPDATE
    public Vector<LSA> lsaArray = null;

    public short weight;

    public SOSPFPacket() {
    }

    private SOSPFPacket(Builder b) {
        srcProcessIP = b.srcProcessIP;
        srcProcessPort = b.srcProcessPort;
        srcIP = b.srcIP;
        dstIP = b.dstIP;
        sospfType = b.sospfType;
        routerID = b.routerID;
        neighborID = b.neighborID;
        lsaArray = b.lsaArray;
        weight = b.weight;
    }

    public static class Builder {

        private String srcProcessIP;
        private short srcProcessPort;
        private String srcIP;
        private String dstIP;
        private short sospfType;
        private String routerID;
        private short weight;

        private String neighborID;
        private Vector<LSA> lsaArray = null;

        public Builder srcProccessIP(String ip) {
            this.srcProcessIP = ip;
            return this;
        }

        public Builder srcProcessPort(short port) {
            this.srcProcessPort = port;
            return this;
        }

        public Builder srcIP(String ip) {
            this.srcIP = ip;
            return this;
        }

        public Builder dstIP(String ip) {
            this.dstIP = ip;
            return this;
        }

        public Builder Hello() {
            this.sospfType = 0;
            return this;
        }

        public Builder LSUPDATE() {
            this.sospfType = 1;
            return this;
        }

        public Builder routerID(String id) {
            this.routerID = id;
            return this;
        }

        public Builder neighborID(String id) {
            this.neighborID = id;
            return this;
        }

        public Builder lsaArray(Vector<LSA> vector) {
            this.lsaArray = vector;
            return this;
        }

        public Builder weight(short weight) {
            this.weight = weight;
            return this;
        }

        public SOSPFPacket build() {
            return new SOSPFPacket(this);
        }

    }

}


