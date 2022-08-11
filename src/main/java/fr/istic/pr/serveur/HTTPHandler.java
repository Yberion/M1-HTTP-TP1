package fr.istic.pr.serveur;

import org.tinylog.Logger;

import java.io.*;
import java.net.Socket;

public class HTTPHandler implements ClientHandler, Runnable {
    private static final String WWW_FOLDER_PATH = "./www";
    private final Socket socket;

    public HTTPHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void handle() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        PrintWriter out = new PrintWriter(this.socket.getOutputStream());

        String tmpLine = "";

        boolean firstLine = true;

        String methode = "";
        String fileName = "";
        //String httpVersion = "";

        // Lecture du header jusqu'à trouver une ligne vide
        while ((tmpLine = in.readLine()).length() != 0) {
            String[] splittedLine = tmpLine.split(" ");

            if (firstLine) {
                if (splittedLine.length != 3) {
                    doError(out);

                    break;
                }

                methode = splittedLine[0];
                fileName = splittedLine[1];
                //httpVersion = splittedLine[2];

                firstLine = false;
            }
        }

        switch (methode) {
            case "GET":
                doGet(WWW_FOLDER_PATH + fileName, out);
                break;
            default:
                doError(out);
        }

        // Also close the [In|Out]putStream
        this.socket.close();
    }

    public void doGet(String pagepath, PrintWriter out) throws IOException {
        File file = new File(pagepath);

        if (!file.exists() || file.isDirectory()) {
            send404(out);

            return;
        }

        StringBuilder htmlPage = new StringBuilder();

        try (BufferedReader buff = new BufferedReader(new FileReader(file))) {
            String line = "";


            while ((line = buff.readLine()) != null) {
                htmlPage.append(line);
            }
        }

        out.print("HTTP/1.1 200 OK\r\n");
        out.print("Content-Type: text/html; charset=utf-8\r\n");
        out.print("Content-Length: " + htmlPage.length() + "\r\n");
        out.print("Connection: close\r\n");
        out.print("\r\n");

        out.flush();

        out.print(htmlPage.toString());
        out.flush();
    }

    public void send404(PrintWriter out) {
        out.print("HTTP/1.1 404 Not Found\r\n");
        out.print("Content-Type: text/html; charset=utf-8\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("\r\n");

        out.flush();
    }

    public void doError(PrintWriter out) {
        out.print("HTTP/1.1 405 Method Not Allowed\r\n");
        out.print("Content-Type: text/html; charset=utf-8\r\n");
        out.print("Allow: GET\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("\r\n");

        out.flush();
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