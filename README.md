# Compte rendu - IPR - TP1 : TCP

### Emily Delorme - Brandon Largeau

# Partie 1 : Le classique serveur Echo

## EXERCICE 1 : Version séquentielle

Classe `fr.istic.pr.echo.EchoServer`

```Java
package fr.istic.pr.echo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.tinylog.Logger;

public class EchoServer
{
	private static final int PORT = 8080;

	public static void main(String[] args) throws IOException
	{
		try (ServerSocket serverSocket = new ServerSocket(PORT))
		{
			int clientNumber = 0;
			
			while (serverSocket.isBound())
			{
			    Logger.info("Attente d'une connexion d'un client");
				
				Socket clientSocket = serverSocket.accept();
				
				Logger.info("Client n°" + clientNumber + " (" + clientSocket.getInetAddress() + ") connecté.");
				
				ClientHandler clientHandler = new ClientHandlerBytes(clientSocket);
				//ClientHandler clientHandler = new ClientHandlerChar(clientSocket);
				
				clientHandler.handle();
				
				++clientNumber;
			}
		}
	}
}
```

Pour éviter le duplicata de code entre la version séquentielle et multithreadé nous avons créé un fichier `fr.istic.pr.ultis.Utils.java` qui contient les fonctions de traitement des données `handleBytes()` et `handleChar()`.

Les classes `fr.istic.pr.echo.ClientHandlerBytes` et `fr.istic.pr.echo.ClientHandlerChar` font appel à ces fonctions utilitaires.

Example avec `fr.istic.pr.echo.ClientHandlerBytes` :

```Java
package fr.istic.pr.echo;

import java.io.IOException;
import java.net.Socket;

import fr.istic.pr.utils.Utils;

public class ClientHandlerBytes implements ClientHandler
{
    private static final int SIZE = 8;
    private static final String SERVER_BROADCAST = "From server: ";
    
    private Socket clientSocket;

    public ClientHandlerBytes(Socket clientSocket)
    {
        this.clientSocket = clientSocket;
    }

    @Override
    public void handle() throws IOException
    {
        Utils.handleBytes(clientSocket, SIZE, SERVER_BROADCAST);
    }
}
```

Et la fonction utilitaire `handleBytes()` :

```Java
public static void handleBytes(Socket clientSocket, int size, String serverBroadcast) throws IOException
{
    byte[] buffer = new byte[size];
    int length = 0;
    int totalSize = 0;
    StringBuilder message = new StringBuilder();

    InputStream in = clientSocket.getInputStream();
    OutputStream out = clientSocket.getOutputStream();

    do
    {
        // We need to catch the Exception here since we just want to exit the while
        // We need to do that in case the client exit without sending an empty message
        try
        {
            // Blocking
            length = in.read(buffer);
        }
        catch (IOException e)
        {
            // Connection reseted by the client (probably the client left netcat)
            length = -1;
        }

        totalSize += length;

        // We only put in the StringBuilder what we've read
        // this will prevent bad behavior on the last call to read 
        // in case lenght < SIZE
        // On Linux netcat probably send an empty message compared
        // to Windows where it totally close the connection
        if (length > 0)
        {
            message.append(new String(buffer).toCharArray(), 0, length);
        }

        // We've read all the data from the client so display it
        if (length < size && length > 0)
        {
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
```

## EXERCICE 2 : Test et capture

Le dialogue se fait correctement avec l'utilisation de netcat ``nc localhost 8080``. Il faut noter que ces screens ont été pris pendant le développement donc il manque `From server: `, etc.

Filtre utilisé sur Wireshark : ``tcp dst port 80``

![P1_Exo2_Wire2](images/Partie%201/exercice_2/Wireshark2.png)

### Le texte de l'échange est-il lisible ?

Oui le texte est lisible.

### Que se passe-t-il quand la taille du message dépasse la taille du buffer ?

Dans notre implémentation et en testant sur Windows (comme je vous l'avais dit en TP) nous n'avons rien constaté de différent.

### La taille des segments TCP correspond-elle à la taille du buffer ?

Non il correspond à la taille du message.

## EXERCICE 3 : Version Multithreadée

Classe `fr.istic.pr.echomt.EchoServerMT`

```Java
package fr.istic.pr.echomt;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.tinylog.Logger;

public class EchoServerMT
{
	private static final int PORT = 8080;
	private static final int NUMBER_THREADS = 4;

	public static void main(String[] args) throws IOException
	{
		try (ServerSocket serverSocket = new ServerSocket(PORT))
		{
			int clientNumber = 0;
			
			Executor threadPool = Executors.newFixedThreadPool(NUMBER_THREADS);
			
			while (serverSocket.isBound())
			{
				Logger.info("Attente d'une connexion d'un client");
				
				Socket clientSocket = serverSocket.accept();
				
				Logger.info("Client n°" + clientNumber + " (" + clientSocket.getInetAddress() + ") connecté.");
				
				//threadPool.execute(new ClientHandlerCharMT(clientSocket));
				threadPool.execute(new ClientHandlerBytesMT(clientSocket));

				++clientNumber;
			}
		}
	}
}
```

La connexion avec plusieurs clients se fait correctement : 

![P1_Exo3_Wire1](images/Partie%201/exercice_3/Wireshark1.png)

# Partie 2 : Implémentation d'un client HTTPping

## EXERCICE 1 : Analyse du protocole HTTP

### Côté client 

### Quel est le rôle de la première ligne ?

Dans le protocole HTTP, une méthode est une commande spécifiant un type de requête, c'est-à-dire qu'elle demande au serveur d'effectuer une action. En général l'action concerne une ressource identifiée par l'URL qui suit le nom de la méthode.

Dans l'illustration ci-contre, une requête GET est envoyée pour récupérer la page d'accueil (utilisation de ``Follow TCP Stream``).

![P2_Exo1_Wire1](images/Partie%202/Exercice%201/1.png)

### Décrivez le rôle des options : "Accept-Encoding:", "Accept:" et "Connection: keep-alive" ?

**Accept-Encoding :** L'en-tête HTTP Accept-Encoding permet de définir quel sera l'encodage du contenu. Il s'agit généralement de l'algorithme de compression utilisé par le serveur. Le client peut alors décoder le corps de la requête correctement. Utilisant la négociation de contenu (content negotiation), le serveur choisit l'une des propositions d'encodage que le client supporte. Le serveur l'utilise et le notifie au client à l'aide de l'en-tête Content-Encoding de la réponse.

**Accept :** Cet en-tête liste les types MIME de contenu acceptés par le client. Le caractère étoile * peut servir à spécifier tous les types / sous-types.
    
**Connection: keep-alive :** Les soucis majeurs des deux premières versions du protocole HTTP sont d'une part le nombre important de connexions lors du chargement d'une page complexe (contenant beaucoup d'images ou d'animations) et d'autre part le temps d'ouverture d'une connexion entre client et serveur (l'établissement d'une connexion TCP prend un temps triple de la latence entre client et serveur). Des expérimentations de connexions persistantes ont cependant été effectuées avec HTTP 1.0 (notamment par l'emploi de l'en-tête Connection: Keep-Alive), mais cela n'a été définitivement mis au point qu'avec HTTP 1.1.

Par défaut, HTTP 1.1 utilise des connexions persistantes, autrement dit la connexion n'est pas immédiatement fermée après une requête, mais reste disponible pour une nouvelle requête. On appelle souvent cette fonctionnalité keep-alive. Il est aussi permis à un client HTTP d'envoyer plusieurs requêtes sur la même connexion sans attendre les réponses. On appelle cette fonctionnalité pipelining. La persistance des connexions permet d'accélérer le chargement de pages contenant plusieurs ressources, tout en diminuant la charge du réseau.

La gestion de la persistance d'une connexion est gérée par l'en-tête Connection.

### Côté serveur

### Décrire la première ligne. A quoi sert le code de réponse ?

Indique qu'on est sur une requête HTTP version 1.1 avec un code d'état.

En informatique, le code HTTP (aussi appelé code d'état) permet de déterminer le résultat d'une requête ou d'indiquer une erreur au client. Ce code numérique est destiné aux traitements automatiques par les logiciels de client HTTP. Ces codes d'état ont été définis par la RFC 2616, en même temps que d’autres codes d'état, non normalisés mais très utilisés sur le Web. Ils ont été ensuite étendus par la RFC 7231.

Le premier chiffre du code d'état est utilisé pour spécifier une des cinq catégories de réponse (informations, succès, redirection, erreur client et erreur serveur).

### Trouvez un site dont la réponse est 404 ou 403.

https://github.com/Yberion_404

https://www.romainbrasier.fr/test


### En vous aidant de la spec décrivez les grandes étapes du protocole pour la récupération d'une page ?

https://developer.mozilla.org/fr/docs/Web/HTTP/Session
    
Dans les protocoles client-serveur, comme HTTP, les sessions se composent de trois phases :

- Le client établit une connexion TCP (ou la connexion appropriée si la couche de transport n'est pas TCP).
- Le client envoie sa requête et attend la réponse.
 - Le serveur traite la requête, renvoyant sa réponse, fournissant un code d'état et des données appropriées.

À partir de HTTP / 1.1, la connexion n'est plus fermée après avoir terminé la troisième phase, et le client peut à nouveau effectuer une requête : cela signifie que la deuxième et la troisième phases peuvent maintenant être effectuées à tout moment.

## Exercice 2 : Récupération d'une page avec netcat

Utilisation de curl (``curl -v www.example.com``) :

![P2_Exo2_1](images/Partie%202/Exercice%202/1.png)

Utilisation de netcat (``nc www.example.com 80``) :

![P2_Exo2_1](images/Partie%202/Exercice%202/2.png)

### example.com supporte-t-il le protocole HTTP/1.0 ?

Oui il supporte HTTP/1.0, on peut le voir dans la réponse.

```
HTTP/1.0 200 OK
Age: 207593
Cache-Control: max-age=604800
Content-Type: text/html; charset=UTF-8
Date: Sat, 29 Feb 2020 23:25:56 GMT
Etag: "3147526947+ident"
Expires: Sat, 07 Mar 2020 23:25:56 GMT
Last-Modified: Thu, 17 Oct 2019 07:18:26 GMT
Server: ECS (bsa/EB15)
Vary: Accept-Encoding
X-Cache: HIT
Content-Length: 1256
Connection: close
```

### Quel est l'encodage utilisé pour le type de retour ?

Il semblerait qu'il n'y ait pas d'encodage au vu de l'absence de "Content-Encoding".

### Que se passe-t-il si vous ajoutez "Accept-Encoding : gzip" ?

Le header est modifié et on peut voir l'ajout de `Content-Encoding: gzip` et `Accept-Ranges: bytes`.

On note aussi que la taille du contenu est réduit. L'utilisation de gzip à compressé les données.

Il est nécessaire de décompression le contenu avec un algorithme de décompression, ici gzip, si on veut pouvoir lire les données.

![P2_Exo2_3](images/Partie%202/Exercice%202/3.png)

## Exercice 3, Implémentation du HTTP ping

Classe `fr.istic.pr.ping.HttpPing` :

```Java
package fr.istic.pr.ping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.tinylog.Logger;

public class HttpPing
{
    public static void main(String[] args) throws IOException
    {
        Logger.info(ping("www.example.com", 80));
    }

    public static PingInfo ping(String host, int port) throws IOException
    {
        PingInfo pingInfo = new PingInfo();
        long start = System.currentTimeMillis();
        
        StringBuilder reponse = new StringBuilder();
        String tmpLine = "";
        boolean firstLine = true;

        try (Socket socket = new Socket(host, port))
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            
            // Entête avec ligne vide pour validation
            out.print("GET / HTTP/1.1\r\n");
            out.print("Host: www.example.com\r\n");
            out.print("Connection: close\r\n");
            out.print("\r\n");
            out.flush();

            // Lecture de la réponse
            while ((tmpLine = in.readLine()) != null)
            {
                if (firstLine)
                {
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

```

La classe `fr.istic.pr.ping.PingInfo` peut contenir le code de réponse et le temps d'exécution de la requête.

Voici la réponse de www.example.com :

```
HTTP/1.1 200 OK
Age: 513276
Cache-Control: max-age=604800
Content-Type: text/html; charset=UTF-8
Date: Wed, 08 Apr 2020 13:42:12 GMT
Etag: "3147526947+ident"
Expires: Wed, 15 Apr 2020 13:42:12 GMT
Last-Modified: Thu, 17 Oct 2019 07:18:26 GMT
Server: ECS (bsa/EB15)
Vary: Accept-Encoding
X-Cache: HIT
Content-Length: 1256
Connection: close

<!doctype html>
<html>
<head>
    <title>Example Domain</title>
...

[Code 200 in 236ms]
```

## Exercice 4 : Socket sécurisée

Changements effectués pour la version sécurisée :

```Java
...

SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();

try (Socket socket = factory.createSocket(host, port))
{
    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    PrintWriter out = new PrintWriter(socket.getOutputStream());

    // Entête avec ligne vide pour validation
    out.print("GET / HTTP/1.0\r\n");
    out.print("Host: www.google.fr\r\n");
    out.print("Connection: close\r\n");
    out.print("\r\n");
    out.flush();

...
```

On fait l'appel avec `ping("www.google.fr", 443)`.

On peut voir qu'on a bien l'utilisation du port 443 et que le protocole TLS (successeur de SSL) est ici utilisé.

![P2_Exo4_1](images/Partie%202/Exercice%204/1.png)

# Partie 3 : Implémentation d'un serveur HTTP simple

Pas grand-chose à dire, lors de nos tests tout fonctionnait.

Classe `fr.istic.pr.serveur.ServeurHTTP` :

```Java
package fr.istic.pr.serveur;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.tinylog.Logger;


public class ServeurHTTP
{
    private static final int PORT = 8080;
    private static final int THREAD_NUMBER = 4;

    public static void main(String[] args) throws IOException
    {
        try (ServerSocket serverSocket = new ServerSocket(PORT))
        {
            int clientNumber = 0;
            
            Executor threadPool = Executors.newFixedThreadPool(THREAD_NUMBER);
            
            while (serverSocket.isBound())
            {
                Logger.info("Attente d'une connexion d'un client");
                
                Socket clientSocket = serverSocket.accept();
                
                Logger.info("Client n°" + clientNumber + " (" + clientSocket.getInetAddress() + ") connecté.");

                threadPool.execute(new HTTPHandler(clientSocket));

                ++clientNumber;
            }
        }
    }
}
```

Classe `fr.istic.pr.serveur.HTTPHandler` :

```Java
package fr.istic.pr.serveur;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.tinylog.Logger;

public class HTTPHandler implements ClientHandler, Runnable
{
    private static final String WWW_FOLDER_PATH = "./www";
    private Socket socket;

    public HTTPHandler(Socket socket)
    {
        this.socket = socket;
    }

    @Override
    public void handle() throws IOException
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        PrintWriter out = new PrintWriter(this.socket.getOutputStream());

        String tmpLine = "";
        
        boolean firstLine = true;

        String methode = "";
        String fileName = "";
        //String httpVersion = "";
        
        // Lecture du header jusqu'à trouver une ligne vide
        while ((tmpLine = in.readLine()).length() != 0)
        {
            String[] splittedLine = tmpLine.split(" ");
            
            if (firstLine)
            {
                if (splittedLine.length != 3)
                {
                    doError(out);
                    
                    break;
                }
                
                methode = splittedLine[0];
                fileName = splittedLine[1];
                //httpVersion = splittedLine[2];

                firstLine = false;
            }
        }
        
        switch (methode)
        {
            case "GET":
                doGet(WWW_FOLDER_PATH + fileName, out);
                break;
            default:
                doError(out);
        }

        // Also close the [In|Out]putStream
        this.socket.close();
    }

    public void doGet(String pagepath, PrintWriter out) throws IOException
    {
        File file = new File(pagepath);

        if (!file.exists() || file.isDirectory())
        {
            send404(out);
            
            return;
        }
        
        StringBuilder htmlPage = new StringBuilder();

        try (BufferedReader buff = new BufferedReader(new FileReader(file)))
        {
            String line = "";
            

            while ((line = buff.readLine()) != null)
            {
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

    public void send404(PrintWriter out)
    {
        out.print("HTTP/1.1 404 Not Found\r\n");
        out.print("Content-Type: text/html; charset=utf-8\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("\r\n");
        
        out.flush();
    }

    public void doError(PrintWriter out)
    {
        out.print("HTTP/1.1 405 Method Not Allowed\r\n");
        out.print("Content-Type: text/html; charset=utf-8\r\n");
        out.print("Allow: GET\r\n");
        out.print("Content-Length: 0\r\n");
        out.print("\r\n");
        
        out.flush();
    }

    @Override
    public void run()
    {
        try
        {
            handle();
        }
        catch (IOException e)
        {
            Logger.trace(e);
        }
    }
}
```