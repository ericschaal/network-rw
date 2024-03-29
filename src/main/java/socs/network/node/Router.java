package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.runner.Broadcast;
import socs.network.runner.Client;
import socs.network.runner.Listener;
import socs.network.util.Configuration;
import socs.network.util.Utility;
import socs.network.util.error.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Vector;

public class Router {

    private static final int MAX_PORTS = 4;

    private final LinkStateDatabase lsd;
    private final RouterDescription rd = new RouterDescription();
    private final Link[] ports = new Link[MAX_PORTS]; // invariant : no duplicates. (careful a->b == b->a !!!)
    private Listener server; // Server thread

    public Router(Configuration config) {

        rd.setSimulatedIPAddress( config.getString("socs.network.router.ip"));
        rd.setProcessIPAddress("127.0.0.1");
        rd.setProcessPortNumber((short) config.getInt("socs.network.router.port"));
        rd.setStatus(RouterStatus.DEFAULT);

        lsd = new LinkStateDatabase(rd);


    }


    public void start() {

        try {
            System.out.print("Initializing server thread ");
            server = new Listener(rd.getProcessPortNumber(), this);
            server.start();

            System.out.println("Done.");
            System.out.println("Listening on " + rd.getProcessIPAddress() + ":" + rd.getProcessPortNumber());
            System.out.print("Simulated IP : " + rd.getSimulatedIPAddress());
            System.out.println();
            System.out.println("Startup complete.");
            System.out.println();


        } catch (IOException e) {
            System.out.println("Failed to start listening. Stacktrace : ");
            e.printStackTrace();
            System.exit(-1);
        }

    }

    /**
     * Returns true if link already exists
     *
     * @param link link to be checked
     * @return true if link already exists
     */
    public synchronized boolean linkExists(final Link link) {

        for (Link el : ports) {
            if (el !=null && el.equals(link)) return true;
        }
        return false;
    }


    /**
     * Returns the port of the link
     * @param link
     * @return port of the link
     * @throws LinkNotAvailable if link doesn't exist
     */
    public synchronized int getLinkId(Link link) throws LinkNotAvailable {
        if (link == null)
            throw new IllegalArgumentException();
        for (int i = 0; i < MAX_PORTS; i++) {
            if (ports[i] != null && ports[i].equals(link))
                return i;
        }

        throw new LinkNotAvailable();
    }


    /**
     * Adds link to the ports array
     *
     * @param link Link to be added
     * Can throw NoPortAvailableException if port array is full
     */
    public synchronized void addLink(final Link link) throws DuplicatedLink, RouterPortsFull {

        if (link == null)
            throw new IllegalArgumentException();

        if (linkExists(link))
            throw new DuplicatedLink();

        for (int i = 0; i < MAX_PORTS; i++) {
            if (ports[i] == null) {
                ports[i] = link;
                return;
            }
        }

        throw new RouterPortsFull();

    }


    /**
     * Updates LS database with link
     * Sequence number is incremented
     * @param link to be added to LSD
     */
    public synchronized void updateLSD(final Link link) {
        LSA lsa = lsd.getFromStore(rd.getSimulatedIPAddress()); // getting my lsa
        assert (lsa != null);
        try {
            LinkDescription linkDescription = new LinkDescription.LinkDescriptionBuilder()
                    .linkID(link.getOtherEnd(getSimulatedIp()).getSimulatedIPAddress())
                    .portNum(getLinkId(link))
                    .tosMetrics(link.getWeight())
                    .build();

            if (lsa.links.contains(linkDescription))
                    lsa.links.remove(linkDescription);


            lsa.links.add(linkDescription);
            lsa.lsaSeqNumber++;
            lsd.addToStore(getSimulatedIp(), lsa);

        } catch (LinkNotAvailable e) {
            System.out.println("Invalid state exception. Add link before updating LSB");
        }
    }

    /**
     * Removes a link
     * @param link to be deleted
     * @return true if deleted successfully
     */
    public synchronized boolean removeLink(Link link) {
        for (int i = 0; i < 4; i++) {
            if (ports[i] != null && ports[i].equals(link)) {
                ports[i] = null;
                return true;
            }
        }
        return false;
    }

    /**
     * output the shortest path to the given destination ip
     * <p/>
     * format: source ip address  -> ip address -> ... -> destination ip
     *
     * @param destinationIP the ip adderss of the destination simulated router
     */
    private void processDetect(String destinationIP) {
        try {

            System.out.println(lsd.getShortestPath(destinationIP));
        } catch (NoPath e) {
            System.out.println("No path available from " + rd.getSimulatedIPAddress() + " to " + destinationIP);
        }
    }

    /**
     * disconnect with the router identified by the given destination ip address
     * Notice: this command should trigger the synchronization of database
     *
     * @param portNumber the port number which the link attaches at
     */
    private void processDisconnect(short portNumber) {
        try {

            Link link = getLink(portNumber);

            LinkDescription linkDescription = new LinkDescription.LinkDescriptionBuilder()
                    .linkID(link.getOtherEnd(getSimulatedIp()).getSimulatedIPAddress())
                    .portNum(getLinkId(link))
                    .tosMetrics(link.getWeight())
                    .build();


            lsd.removeLinkFromStore(getSimulatedIp(), linkDescription);
            lsd.removeFromStore(link.getOtherEnd(getSimulatedIp()).getSimulatedIPAddress());

            Vector<LSA> lsas = new Vector(lsd.getAllLSA());
            getLsd().removeFromStore(link.getOtherEnd(getSimulatedIp()).getSimulatedIPAddress());

            Broadcast broadcast = new Broadcast(getTwoWayLinks(), lsas, this);
            broadcast.start();
            broadcast.join();

            removeLink(link);


        }
        catch (LinkNotAvailable e) {
            System.out.println("Link does not exist.");
        }
        catch (DatabaseException e) {
            System.out.println(e.getMessage());
        }
        catch (InterruptedException e) {}


    }

    /**
     * attach the link to the remote router, which is identified by the given simulated ip;
     * to establish the connection via socket, you need to identify the process IP and process Port;
     * additionally, weight is the cost to transmitting data through the link
     * <p/>
     * NOTE: this command should not trigger link database synchronization
     */
    // TODO fix this function
    private synchronized Link processAttach(String processIP, short processPort, String simulatedIP, short weight) {

        // args check
        if (processIP == null
                || simulatedIP == null
                || processPort < 0
                || weight < 0
                || !Utility.validateIP(processIP)
                || !Utility.validateIP(simulatedIP)) {

            throw new IllegalArgumentException("Invalid argument");
        }

        // Building description of the other end
        RouterDescription r2 = new RouterDescription.RouterDescriptionBuilder()
                .DEFAULT()
                .processIPAddress(processIP)
                .processPortNumber(processPort)
                .simulatedIPAddress(simulatedIP)
                .build();

        Link newLink = new Link(rd, r2, weight);



        try {

            addLink(newLink);


        } catch (DuplicatedLink e) {
            System.out.println("Duplicated link, link not added.");
        } catch (RouterPortsFull e) {
            System.out.println("No available port, link not added");
        }

        return newLink;

    }

    /**
     * broadcast Hello to neighbors asynchronously
     * A thread is started for each link
     * Thread is interrupted if the connection is fully established (TWO_WAY) or if an exception occurred
     * Socket is not kept alive.
     */
    private void processStart() {

        LinkedList<Client> clients = new LinkedList<Client>();

        // init Hello exchange
        for (Link link : ports) {
            if (link != null && link.getOtherEnd(getSimulatedIp()).getStatus() != RouterStatus.TWO_WAY) {
                Client client = new Client(this, link);
                clients.add(client);
                client.start();
                updateLSD(link);
            }
        }

        int maxSeq = lsd.getAllLSA().stream().max(Comparator.comparingInt((a) -> a.lsaSeqNumber)).get().lsaSeqNumber;
        lsd.getFromStore(getSimulatedIp()).lsaSeqNumber = maxSeq+1;
        // broadcast LS updates to neighbors
        try {

            for (Client client : clients)
                client.join();

            Vector<LSA> lsas = new Vector<LSA>(lsd.getAllLSA());

            Broadcast broadcast = new Broadcast(getTwoWayLinks(), lsas, this);
            broadcast.start();

        } catch (InterruptedException e) {}


    }


    /**
     * Find all TWO_WAY links
     * @return all TWO_WAY links
     */
    private synchronized Vector<Link> getTwoWayLinks() {
        Vector<Link> vector = new Vector();
        for (Link link : ports) {
            if (link != null && link.getOtherEnd(getSimulatedIp()).getStatus() == RouterStatus.TWO_WAY)
                vector.add(link);
        }
        return vector;
    }


    /**
     * Find a link by its process port
     * @param processPort
     * @return Link that corresponds to proccessPort
     * @throws LinkNotAvailable
     */
    public synchronized Link getLink(short processPort) throws LinkNotAvailable {
        for (Link link : ports) {
            if (link != null) {
                if (link.getOtherEnd(getSimulatedIp()).getProcessPortNumber() == processPort)
                    return link;
            }
        }
        throw new LinkNotAvailable();
    }

    public synchronized Link getLink(String destinationIP) throws LinkNotAvailable {
        for (Link link : ports) {
            if (link != null) {
                if (link.getOtherEnd(getSimulatedIp()).getSimulatedIPAddress().equals(destinationIP))
                    return link;
            }
        }
        throw new LinkNotAvailable();
    }


    /**
     * attach the link to the remote router, which is identified by the given simulated ip;
     * to establish the connection via socket, you need to identify the process IP and process Port;
     * additionally, weight is the cost to transmitting data through the link
     * <p/>
     * This command does trigger the link database synchronization
     */
    //TODO test this.
    private void processConnect(String processIP, short processPort,
                                String simulatedIP, short weight) {
        Link link = processAttach(processIP, processPort, simulatedIP, weight); // attach to router

        // initiate connection to router
        if (link != null && link.getOtherEnd(getSimulatedIp()).getStatus() != RouterStatus.TWO_WAY) {
            Client client = new Client(this, link);
            client.start();
            updateLSD(link);

            try {
                client.join();
            } catch (InterruptedException e) {}

            int maxSeq = lsd.getAllLSA().stream().max(Comparator.comparingInt((a) -> a.lsaSeqNumber)).get().lsaSeqNumber;
            lsd.getFromStore(getSimulatedIp()).lsaSeqNumber = maxSeq+1;

            // database sync
            Vector<LSA> lsas = new Vector<LSA>(lsd.getAllLSA());
            Broadcast broadcast = new Broadcast(getTwoWayLinks(), lsas, this);
            broadcast.start();
        }

    }

    /**
     * Outputs the neighbors of the routers
     */
    private void processNeighbors() {
        for (Link link : ports) {
            if (!Objects.isNull(link) && link.getOtherEnd(getSimulatedIp()).getStatus() == RouterStatus.TWO_WAY)
                System.out.println(link.getOtherEnd(getSimulatedIp()).getSimulatedIPAddress());
        }

    }

    /**
     * Disconnects with all neighbors and quit the program
     */
    private void processQuit() {
        for (Link link : ports) {
            if (link != null) {
                processDisconnect(link.getOtherEnd(this.getSimulatedIp()).getProcessPortNumber());
            }
        }
        System.exit(0);

    }

    /**
     * Simple terminal for router
     * Do not use UnsafeXXX commands if you don't what you are doing
     */
    public void terminal() {
        try {
            InputStreamReader isReader = new InputStreamReader(System.in);
            BufferedReader br = new BufferedReader(isReader);
            System.out.print(">> ");
            String command = br.readLine();
            while (true) {
                if (command.startsWith("detect ")) {
                    String[] cmdLine = command.split(" ");
                    processDetect(cmdLine[1]);
                } else if (command.startsWith("disconnect ")) {
                    String[] cmdLine = command.split(" ");
                    processDisconnect(Short.parseShort(cmdLine[1]));
                } else if (command.startsWith("quit")) {
                    processQuit();
                } else if (command.startsWith("attach ")) {
                    String[] cmdLine = command.split(" ");
                    processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                            cmdLine[3], Short.parseShort(cmdLine[4]));
                } else if (command.equals("start")) {
                    processStart();
                } else if (command.startsWith("connect ")) {
                    String[] cmdLine = command.split(" ");
                    processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                            cmdLine[3], Short.parseShort(cmdLine[4]));
                } else if (command.equals("neighbors")) {
                    //output neighbors
                    processNeighbors();
                } else if (command.equals("list")) { // debug
                    printPorts();
                }
                else if (command.equals("lsd")) { // debug
                  printLSD();
                } else if (command.startsWith("UnsafeRemove ")) {
                    String[] cmdLine = command.split(" ");
                    removeLink(Integer.parseInt(cmdLine[1]));
                }
                else {
                    System.out.println("Invalid command");
                    break;
                }
                System.out.print(">> ");
                command = br.readLine();
            }
            isReader.close();
            br.close();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Prints all links
     */
    private synchronized void printPorts() {
        int i = 0;
        System.out.println();
        for (Link link : ports) {
            if (link == null)
                continue;
            System.out.println("Link " + i);
            System.out.println("R1 : " + link.getRouter1().getSimulatedIPAddress() + ", " + link.getRouter1().getStatus());
            System.out.println("R2 : " + link.getRouter2().getSimulatedIPAddress() + ", " + link.getRouter2().getStatus());
            System.out.println();
            i++;
        }
    }

    /**
     * Debug function. Do not use.
     * Network can end up in a bad state.
     * @param i index of link to be removed
     */
    private synchronized void removeLink(int i) {
        if (i >= 4 || i < 0)
            throw new IllegalArgumentException("Invalid index");
        ports[i] =null;

    }


    public RouterDescription getRd() {
        return rd;
    }

    public String getRealIp() {
        return rd.getProcessIPAddress();
    }

    public short getPort() {
        return rd.getProcessPortNumber();
    }

    public String getSimulatedIp() {
        return rd.getSimulatedIPAddress();
    }

    public LinkStateDatabase getLsd() {
        return lsd;
    }

    public synchronized Link[] getPorts() {
        return ports;
    }

    public synchronized void printLSD() {
        System.out.println();
        System.out.println(lsd.toString());
        System.out.println();
    }


}
