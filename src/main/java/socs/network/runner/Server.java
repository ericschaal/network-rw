package socs.network.runner;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.node.Link;
import socs.network.node.Router;
import socs.network.node.RouterDescription;
import socs.network.node.RouterStatus;
import socs.network.util.Utility;
import socs.network.util.error.DatabaseException;
import socs.network.util.error.DuplicatedLink;
import socs.network.util.error.LinkNotAvailable;
import socs.network.util.error.RouterPortsFull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Created by ericschaal on 2017-02-28.
 * Handles received SOSPFPackets
 */
public class Server extends Thread {


    private final Socket client;
    private final Router owner;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    private RouterDescription sender;
    private Link link;
    private SOSPFPacket rcv;

    public Server(Socket accepted, Router owner) {
        super();
        this.client = accepted;
        this.owner = owner;
    }


    /**
     * Opens socket streams
     * @throws IOException
     */
    private void init() throws IOException {
        out = new ObjectOutputStream(client.getOutputStream());
        in = new ObjectInputStream(client.getInputStream());
    }

    /**
     * Handles first hello packet
     * @throws DuplicatedLink link already exists
     * @throws RouterPortsFull no port available
     * @throws IOException stream error
     */
    private void handleFirstHello() throws DuplicatedLink, RouterPortsFull, IOException {

        System.out.println("received HELLO from " + rcv.srcIP);

        sender = new RouterDescription.RouterDescriptionBuilder() // other end description
                .INIT()
                .processIPAddress(rcv.srcProcessIP)
                .processPortNumber(rcv.srcProcessPort)
                .simulatedIPAddress(rcv.srcIP)
                .build();

        link = new Link(owner.getRd(), sender, rcv.weight);

        if (!owner.linkExists(link)) {

            owner.addLink(link);
            owner.updateLSD(link);

        } else
            link.getOtherEnd(owner.getSimulatedIp()).setStatus(RouterStatus.INIT); // Setting to init

        System.out.println("set " + rcv.srcIP + " to INIT"); // Print log

        // Building packet
        SOSPFPacket packet = new SOSPFPacket.Builder()
                .Hello()
                .srcIP(owner.getSimulatedIp())
                .dstIP(rcv.srcIP)
                .srcProccessIP(owner.getRealIp())
                .srcProcessPort(owner.getPort())
                .routerID(owner.getSimulatedIp())
                .neighborID(owner.getSimulatedIp())
                .build();

        out.writeObject(packet); // sending packet


    }

    /**
     * Handles second Hello packet and initiate LSUpdate broadcast
     * @throws ClassNotFoundException serialization error
     * @throws IOException stream error
     * @throws InterruptedException can't be interrupted
     */
    private void handleSecondHello() throws ClassNotFoundException, IOException, InterruptedException {

        rcv = (SOSPFPacket) in.readObject();

        System.out.println("received HELLO from " + rcv.srcIP); // Printing log

        RouterDescription src = link.getOtherEnd(owner.getSimulatedIp()); // get other end
        src.setStatus(RouterStatus.TWO_WAY); // update other end

        System.out.println("set " + rcv.srcIP + " to TWO_WAY"); // Printing log

        owner.updateLSD(link);



        Vector<Link> links = new Vector<>(Arrays.stream(owner.getPorts()) // get two_way links
                .filter(el -> !Objects.isNull(el) && el.getOtherEnd(owner.getSimulatedIp()).getStatus() == RouterStatus.TWO_WAY)
                .collect(Collectors.toSet()));
        Vector<LSA> lsas = new Vector<>(owner.getLsd().getAllLSA());


        Broadcast broadcast = new Broadcast(links, lsas, owner);
        broadcast.start();
        broadcast.join(); // wait before continuing

    }




    @Override
    public void run() {
        super.run();

        try {

            init();
            this.rcv = (SOSPFPacket) in.readObject();


            switch (Utility.getSOSPFPacketType(rcv)) {
                case HELLO:

                    handleFirstHello();
                    handleSecondHello();
                    break;

                case LSUPDATE:

                    handleLSUpdate();
                    break;

                case UNKNOWN:
                    System.out.println("Invalid packet type.");
                    break;
            }

            client.close();


        } catch (IOException e) {
            System.out.print("Failed to open streams. Stacktrace: ");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Error while serializing. Stacktrace: ");
            e.printStackTrace();
        } catch (RouterPortsFull e) {
            System.out.println("Error. Corrupted state. Stacktrace: ");
            e.printStackTrace();
        } catch (DuplicatedLink e) {
            System.out.println("Error. Corrupted state. Stacktrace: ");
            e.printStackTrace();
        } catch (InterruptedException e) {}


    }

    /**
     * Handles LSU packet
     */
    private void handleLSUpdate() {


        Vector<LSA> lsas = rcv.lsaArray;
        boolean flag = false;

        for (LSA lsa : lsas) {
            if (owner.getLsd().getFromStore(lsa.linkStateID) == null
                    || (owner.getLsd().getFromStore(lsa.linkStateID).lsaSeqNumber < lsa.lsaSeqNumber)) { // no record from this router yet or higher sequence number


                if (owner.getLsd().getFromStore(lsa.linkStateID) != null && !lsa.delete_ack) {
                    LinkedList<LinkDescription> removed = getRemoved(owner.getLsd().getFromStore(lsa.linkStateID).links, lsa.links);
                    if (removed.size() == 1) {
                        try {
                            if (removed.get(0).getLinkID().equals(owner.getSimulatedIp())) { // is my neighbor
                                Link toDelete = owner.getLink(lsa.linkStateID);
                                LinkDescription ld = new LinkDescription.LinkDescriptionBuilder()
                                        .linkID(toDelete.getOtherEnd(owner.getSimulatedIp()).getSimulatedIPAddress())
                                        .portNum(owner.getLinkId(toDelete))
                                        .tosMetrics(toDelete.getWeight())
                                        .build();

                                owner.getLsd().removeLinkFromStore(owner.getSimulatedIp(), ld);

                                 LSA newLsa = owner.getLsd().getFromStore(owner.getSimulatedIp());
                                 newLsa.delete_ack = true;
                                 Vector<LSA> vLSA = new Vector<>();
                                 vLSA.add(newLsa);
                                Vector<Link> vLink = new Vector(Arrays.stream(owner.getPorts())
                                        .filter( el -> {
                                            if (Objects.isNull(el))
                                                return false;
                                            else if (el.getOtherEnd(owner.getSimulatedIp()).getSimulatedIPAddress().equals(rcv.srcIP))
                                                return false;
                                            return true;
                                        }).collect(Collectors.toSet())
                                );
                                 Broadcast broadcast = new Broadcast(vLink, vLSA, owner);
                                 broadcast.start();
                                 broadcast.join();
                                 owner.getPorts()[owner.getLinkId(toDelete)] = null;
                                 flag = true;
                                 owner.getLsd().removeFromStore(lsa.linkStateID);
                            }
                        }
                        catch (InterruptedException e) {}
                        catch (DatabaseException e) {
                            System.out.println("DB error.");
                        }
                        catch (LinkNotAvailable e) {
                            System.out.println("Link not available.");
                        }
                    }
                }


                if (!flag)
                    owner.getLsd().addToStore(lsa.linkStateID, lsa);

                Vector<Link> links = new Vector(Arrays.stream(owner.getPorts())
                        .filter( el -> {
                            if (Objects.isNull(el))
                                return false;
                            else if (el.getOtherEnd(owner.getSimulatedIp()).getSimulatedIPAddress().equals(rcv.srcIP))
                                return false;
                            else if (el.getOtherEnd(owner.getSimulatedIp()).getStatus() != RouterStatus.TWO_WAY)
                                return false;
                            return true;
                        }).collect(Collectors.toSet())
                );


                Broadcast broadcast = new Broadcast(links, lsas, owner);
                broadcast.start();
            }
            else {
                //System.out.println("Dropping.");
            }

        }
    }



    private LinkedList<LinkDescription> getRemoved(LinkedList<LinkDescription> oldD, LinkedList<LinkDescription> newD) {
        LinkedList<LinkDescription> inter = new LinkedList<>();
        for (LinkDescription ld : oldD) {
            if (!newD.contains(ld))
                inter.add(ld);
        }
        return inter;
    }

}
