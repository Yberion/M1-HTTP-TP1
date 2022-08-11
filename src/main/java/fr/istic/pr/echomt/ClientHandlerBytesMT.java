package fr.istic.pr.echomt;

import fr.istic.pr.utils.Utils;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.Socket;

public class ClientHandlerBytesMT implements ClientHandler, Runnable {
    private static final int SIZE = 8;
    private static final String SERVER_BROADCAST = "From server: ";

    private final Socket clientSocket;

    public ClientHandlerBytesMT(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void handle() throws IOException {
        Utils.handleBytes(clientSocket, SIZE, SERVER_BROADCAST);
    }

    @Override
    public void run() {
        try {
            this.handle();
        } catch (IOException e) {
            Logger.trace(e);
        }
    }
}
