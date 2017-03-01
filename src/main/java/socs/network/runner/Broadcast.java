package socs.network.runner;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;
import socs.network.node.Link;
import socs.network.node.Router;
import socs.network.node.RouterDescription;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Vector;

/**
 * Created by ericschaal on 2017-02-28.
 */
public class Broadcast extends Thread {

    private final Vector<Link> links;
    private final Vector<LSA> lsas;
    private final Router owner;
    private Socket client;

    private ObjectOutputStream out;


    public Broadcast(Vector<Link> links, Vector<LSA> lsas, Router owner) {
        super();
        this.links = links;
        this.lsas = lsas;
        this.owner = owner;
    }

    @Override
    public void run() {
        super.run();


        for (Link link : links) {

            try {


                RouterDescription destination = link.getOtherEnd(owner.getSimulatedIp());
                client = new Socket(destination.getProcessIPAddress(), destination.getProcessPortNumber());
                out = new ObjectOutputStream(client.getOutputStream());

                SOSPFPacket sospfPacket = new SOSPFPacket.Builder()
                        .LSUPDATE()
                        .srcIP(owner.getSimulatedIp())
                        .dstIP(destination.getSimulatedIPAddress())
                        .srcProcessPort(owner.getPort())
                        .srcProccessIP(owner.getRealIp())
                        .routerID(owner.getSimulatedIp())
                        .neighborID(owner.getSimulatedIp())
                        .lsaArray(lsas)
                        .build();

                out.writeObject(sospfPacket);


            } catch (IOException e) {
                System.out.println("Failed to create socket.");
                if (e instanceof ConnectException) {
                    System.out.println(link.getOtherEnd(owner.getSimulatedIp()) + ". Host down.");
                }
            }
        }
    }
}
