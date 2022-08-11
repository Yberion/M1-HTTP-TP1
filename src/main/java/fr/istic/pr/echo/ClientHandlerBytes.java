package fr.istic.pr.echo;

import fr.istic.pr.utils.Utils;

import java.io.IOException;
import java.net.Socket;

public class ClientHandlerBytes implements ClientHandler {
    private static final int SIZE = 8;
    private static final String SERVER_BROADCAST = "From server: ";

    private final Socket clientSocket;

    public ClientHandlerBytes(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void handle() throws IOException {
        Utils.handleBytes(clientSocket, SIZE, SERVER_BROADCAST);
    }
}
