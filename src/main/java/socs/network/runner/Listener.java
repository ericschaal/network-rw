package socs.network.runner;

import socs.network.node.Router;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by ericschaal on 2017-02-28.
 */
public class Listener extends Thread {

    private final Router owner;
    private final short listeningPort;
    private final ServerSocket serverSocket;

    public Listener(short port, Router owner) throws IOException {
        super();
        this.listeningPort = port;
        this.owner = owner;
        this.serverSocket = new ServerSocket(this.listeningPort);
    }


    @Override
    public void run() {
        super.run();

        while (!interrupted()) {

            try {

                Socket clientSocket = serverSocket.accept();
                Server server = new Server(clientSocket, owner);
                server.start();


            } catch (IOException e) {
                System.out.println("Exception occurred on accept.");
            }


        }

        try {
            serverSocket.close();
            System.out.println("Listening thread stopped");
        } catch (Exception e) {
            System.out.println("Failed to stop listening thread");
            System.exit(-1);
        }

    }



}
