package fr.istic.pr.echo;

import java.io.IOException;

public interface ClientHandler {
    /**
     * La méthode handle traite le client
     *
     * @throws IOException
     **/
    void handle() throws IOException;
}