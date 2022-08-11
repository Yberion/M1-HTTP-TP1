package fr.istic.pr.serveur;

import org.tinylog.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Affiche {
    public static void main(String[] args) throws IOException {
        String path = "./www/";
        String fileName = "test.html";

        File file = new File(path + fileName);

        if (!file.exists() || file.isDirectory()) {
            return;
        }

        Logger.info(file.getAbsolutePath());
        Logger.info(file.exists());

        try (BufferedReader fin = new BufferedReader(new FileReader(file))) {
            String line = "";
            StringBuilder htmlPage = new StringBuilder();

            while ((line = fin.readLine()) != null) {
                htmlPage.append(line + "\n");
            }

            Logger.info(htmlPage);
        }
    }
}