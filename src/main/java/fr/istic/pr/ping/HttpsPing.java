package fr.istic.pr.ping;

import org.tinylog.Logger;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class HttpsPing {
    public static void main(String[] args) throws IOException {
        Logger.info(ping("www.google.fr", 443));
    }

    public static PingInfo ping(String host, int port) throws IOException {
        PingInfo pingInfo = new PingInfo();
        long start = System.currentTimeMillis();

        StringBuilder reponse = new StringBuilder();
        String tmpLine = "";
        boolean firstLine = true;

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        try (Socket socket = factory.createSocket(host, port)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            // Entête avec ligne vide pour validation
            out.print("GET / HTTP/1.0\r\n");
            out.print("Host: www.google.fr\r\n");
            out.print("Connection: close\r\n");
            out.print("\r\n");
            out.flush();

            // Lecture de la réponse
            while ((tmpLine = in.readLine()) != null) {
                if (firstLine) {
                    String[] splittedLine = tmpLine.split(" ");

                    pingInfo.setCode(Integer.valueOf(splittedLine[1]));

                    firstLine = false;
                }

                reponse.append(tmpLine);
                reponse.append("\n");
            }

            // Affichage de la réponse
            Logger.info(reponse);
        }

        pingInfo.setTime(System.currentTimeMillis() - start);

        return pingInfo;
    }
}
