package fr.istic.pr.echomt;

import org.tinylog.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EchoServerMT {
    private static final int PORT = 8080;
    private static final int NUMBER_THREADS = 4;

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            int clientNumber = 0;

            Executor threadPool = Executors.newFixedThreadPool(NUMBER_THREADS);

            while (serverSocket.isBound()) {
                Logger.info("Attente d'une connexion d'un client");

                Socket clientSocket = serverSocket.accept();

                Logger.info("Client n°" + clientNumber + " (" + clientSocket.getInetAddress() + ") connecté.");

                //threadPool.execute(new ClientHandlerCharMT(clientSocket));
                threadPool.execute(new ClientHandlerBytesMT(clientSocket));

                ++clientNumber;
            }
        }
    }
}