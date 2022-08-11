package fr.istic.pr.utils;

import org.tinylog.Logger;

import java.io.*;
import java.net.Socket;

// Remove duplication of code
public interface Utils {
    static void handleBytes(Socket clientSocket, int size, String serverBroadcast) throws IOException {
        byte[] buffer = new byte[size];
        int length = 0;
        int totalSize = 0;
        StringBuilder message = new StringBuilder();

        InputStream in = clientSocket.getInputStream();
        OutputStream out = clientSocket.getOutputStream();

        do {
            // We need to catch the Exception here since we just want to exit the while
            // We need to do that in case the client exit without sending an empty message
            try {
                // Blocking
                length = in.read(buffer);
            } catch (IOException e) {
                // Connection reseted by the client (probably the client left netcat)
                length = -1;
            }

            totalSize += length;

            // We only put in the StringBuilder what we've read
            // this will prevent bad behavior on the last call to read 
            // in case lenght < SIZE
            // On Linux netcat probably send an empty message compared
            // to Windows where it totally close the connection
            if (length > 0) {
                message.append(new String(buffer).toCharArray(), 0, length);
            }

            // We've read all the data from the client so display it
            if (length < size && length > 0) {
                // To add "SERVER_BROADCAST" in the response
                StringBuilder sbTmp = new StringBuilder();

                String tmpMessage = message.toString();

                Logger.info("From client: " + tmpMessage);

                sbTmp.append(serverBroadcast);
                sbTmp.append(tmpMessage);

                // Send back the message
                // we only send the total length of what we've read
                out.write(sbTmp.toString().getBytes(), 0, serverBroadcast.length() + totalSize);

                // allocate a new StringBuilder since there is no proper way to clear it
                message = new StringBuilder();

                // Clear the total length of what we've read
                totalSize = 0;
            }
        } while (length != -1);

        // Also close the [In|Out]putStream
        clientSocket.close();
    }

    static void handleChar(Socket clientSocket) throws IOException {
        String message = "";

        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream());

        try {
            while ((message = in.readLine()) != null) {
                Logger.info(message);

                out.println(message);
                out.flush();
            }
        } catch (IOException e) {
            // Connection reseted by the client (probably the client left netcat)
            // We do nothing
        }

        // Also close the [In|Out]putStream
        clientSocket.close();
    }
}