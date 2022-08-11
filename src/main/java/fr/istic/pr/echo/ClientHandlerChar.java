package fr.istic.pr.echo;

import fr.istic.pr.utils.Utils;

import java.io.IOException;
import java.net.Socket;

public class ClientHandlerChar implements ClientHandler {
    private final Socket clientSocket;

    public ClientHandlerChar(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void handle() throws IOException {
        Utils.handleChar(clientSocket);
    }
}
