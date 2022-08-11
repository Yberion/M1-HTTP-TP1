package fr.istic.pr.echo;

import org.tinylog.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            int clientNumber = 0;

            while (serverSocket.isBound()) {
                Logger.info("Attente d'une connexion d'un client");

                Socket clientSocket = serverSocket.accept();

                Logger.info("Client n°" + clientNumber + " (" + clientSocket.getInetAddress() + ") connecté.");

                ClientHandler clientHandler = new ClientHandlerBytes(clientSocket);
                //ClientHandler clientHandler = new ClientHandlerChar(clientSocket);

                clientHandler.handle();

                ++clientNumber;
            }
        }
    }
}