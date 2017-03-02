package socs.network.runner;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;
import socs.network.node.Link;
import socs.network.node.Router;
import socs.network.node.RouterDescription;
import socs.network.node.RouterStatus;
import socs.network.util.Utility;
import socs.network.util.error.DuplicatedLink;
import socs.network.util.error.RouterPortsFull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.Vector;
import java.util.stream.Collectors;

/**
 * Created by ericschaal on 2017-02-28.
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


    private void init() throws IOException {
        out = new ObjectOutputStream(client.getOutputStream());
        in = new ObjectInputStream(client.getInputStream());
    }

    private void handleFirstHello() throws DuplicatedLink, RouterPortsFull, IOException {

        System.out.println("received HELLO from " + rcv.srcIP);

        sender = new RouterDescription.RouterDescriptionBuilder()
                .INIT()
                .processIPAddress(rcv.srcProcessIP)
                .processPortNumber(rcv.srcProcessPort)
                .simulatedIPAddress(rcv.srcIP)
                .build();

        link = new Link(owner.getRd(), sender, rcv.weight);

        if (!owner.linkExists(link)) {

            owner.addLink(link);
            owner.updateLSD(link);
        } else link.getOtherEnd(owner.getSimulatedIp()).setStatus(RouterStatus.INIT); // Setting to init

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

        // send lsupdate


    }

    private void handleSecondHello() throws ClassNotFoundException, IOException, InterruptedException {

        rcv = (SOSPFPacket) in.readObject();

        System.out.println("received HELLO from " + rcv.srcIP); // Printing log

        RouterDescription src = link.getOtherEnd(owner.getSimulatedIp()); // get other end
        src.setStatus(RouterStatus.TWO_WAY); // update other end

        System.out.println("set " + rcv.srcIP + " to TWO_WAY"); // Printing log

        owner.updateLSD(link);



        Vector<Link> links = new Vector<>(Arrays.stream(owner.getPorts())
                .filter(el -> !Objects.isNull(el))
                .collect(Collectors.toSet()));
        Vector<LSA> lsas = new Vector<>(owner.getLsd().getAllLSA());


        Broadcast broadcast = new Broadcast(links, lsas, owner);
        broadcast.start();
        broadcast.join();

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

                    System.out.println("Received LSUPDATE from " + rcv.srcIP);

                    Vector<LSA> lsas = rcv.lsaArray;

                    for (LSA lsa : lsas) {

                        //System.out.println("LSA vector size: " + lsas.size());
                        if (owner.getLsd().getFromStore(lsa.linkStateID) == null
                                || (owner.getLsd().getFromStore(lsa.linkStateID).lsaSeqNumber < lsa.lsaSeqNumber)) { // no record from this router yet or newest sequence number

                            System.out.println("Owner is :" + lsa.linkStateID);

                            owner.getLsd().addToStore(lsa.linkStateID, lsa);

                            // TODO fix filtering. Actually. Was working properly with lambdas..

                            Vector<Link> links = new Vector(Arrays.stream(owner.getPorts())
                                    .filter( el -> {
                                        if (Objects.isNull(el))
                                            return false;
                                        else if (el.getOtherEnd(owner.getSimulatedIp()).getSimulatedIPAddress().equals(rcv.srcIP))
                                            return false;
                                        return true;
                                    }).collect(Collectors.toSet())
                            );

                            for (Link link : links)
                                    System.out.println("Broadcasting to: " + link.getOtherEnd(owner.getSimulatedIp()).getSimulatedIPAddress());

                            Broadcast broadcast = new Broadcast(links, lsas, owner);
                            broadcast.start();
                        }
                        else {
                            //System.out.println("Dropping.");
                        }

                    }
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
}
