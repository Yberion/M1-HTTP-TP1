package fr.istic.pr.echomt;

import fr.istic.pr.utils.Utils;
import org.tinylog.Logger;

import java.io.IOException;
import java.net.Socket;

public class ClientHandlerCharMT implements ClientHandler, Runnable {
    private final Socket clientSocket;

    public ClientHandlerCharMT(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void handle() throws IOException {
        Utils.handleChar(clientSocket);
    }

    @Override
    public void run() {
        try {
            handle();
        } catch (IOException e) {
            Logger.trace(e);
        }
    }
}
