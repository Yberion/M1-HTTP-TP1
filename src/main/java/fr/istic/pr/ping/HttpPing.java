package fr.istic.pr.ping;

import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class HttpPing {
    public static void main(String[] args) throws IOException {
        Logger.info(ping("www.example.com", 80));
    }

    public static PingInfo ping(String host, int port) throws IOException {
        PingInfo pingInfo = new PingInfo();
        long start = System.currentTimeMillis();

        StringBuilder reponse = new StringBuilder();
        String tmpLine = "";
        boolean firstLine = true;

        try (Socket socket = new Socket(host, port)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());

            // Entête avec ligne vide pour validation
            out.print("GET / HTTP/1.1\r\n");
            out.print("Host: www.example.com\r\n");
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
