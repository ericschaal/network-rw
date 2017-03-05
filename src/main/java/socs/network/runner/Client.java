package socs.network.runner;

import socs.network.message.SOSPFPacket;
import socs.network.message.SOSPFPacketType;
import socs.network.node.Link;
import socs.network.node.Router;
import socs.network.node.RouterDescription;
import socs.network.node.RouterStatus;
import socs.network.util.Utility;
import socs.network.util.error.UnexpectedSOSFPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Created by ericschaal on 2017-02-28.
 */
public class Client extends Thread {

    private final Router owner;
    private final Link link;
    private final RouterDescription destination;

    private Socket client;
    private ObjectInputStream in;
    private ObjectOutputStream out;


    public Client(Router owner, Link link) {
        super();
        this.owner = owner;
        this.link = link;
        this.destination = link.getOtherEnd(owner.getSimulatedIp());

    }


    /**
     * Opens socket and IO streams
     * @throws IOException IO stream error
     */
    private void init() throws IOException {
        this.client = new Socket(destination.getProcessIPAddress(), destination.getProcessPortNumber());
        out = new ObjectOutputStream(client.getOutputStream());
        in = new ObjectInputStream(client.getInputStream());
    }

    /**
     * Sends Hello packet
     * @throws IOException
     */
    private void sendHello() throws IOException {
        SOSPFPacket hello = new SOSPFPacket.Builder()
                .Hello()
                .srcIP(owner.getSimulatedIp())
                .dstIP(destination.getSimulatedIPAddress())
                .srcProcessPort(owner.getPort())
                .srcProccessIP(owner.getRealIp())
                .routerID(owner.getSimulatedIp())
                .neighborID(owner.getSimulatedIp())
                .weight(link.getWeight())
                .build();

        out.writeObject(hello);
    }

    /**
     * Block until packet received
     * @return received packet
     * @throws IOException IO stream error
     * @throws ClassNotFoundException Serialization error
     */
    private SOSPFPacket receiveSOSFPacket() throws IOException, ClassNotFoundException {
        return (SOSPFPacket) in.readObject();
    }

    /**
     * Sets link as Two Way
     */
    private void updateTwoWay() {
        link.getOtherEnd(owner.getSimulatedIp()).setStatus(RouterStatus.TWO_WAY);
    }


    @Override
    public void run() {
        super.run();
        try {

            SOSPFPacket rcv;

            init(); // create socket and open I/O streams


            sendHello();

            rcv = receiveSOSFPacket(); // blocking

            if (Utility.getSOSPFPacketType(rcv) != SOSPFPacketType.HELLO)
                throw new UnexpectedSOSFPacket();

            System.out.println("received HELLO from " + rcv.srcIP);

            updateTwoWay();

            System.out.println("set " + rcv.srcIP + " to TWO_WAY");

            sendHello();



        } catch (IOException e) {
            System.out.print("IO stream error. Stacktrace: ");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            System.out.println("Serialization error. Stacktrace:");
            e.printStackTrace();
        }
    }
}
