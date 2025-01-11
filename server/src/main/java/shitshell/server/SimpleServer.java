package shitshell.server;

import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.*;

public class SimpleServer {

    public static void startServer() throws IOException {
        // Create an HTTP server on port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        System.out.println("Server started on port 8080...");

        // Define routes and handlers
        server.createContext("/auth", new AuthHandler());

        // Start the server
        server.start();
        System.out.println("Routing is now active. Listening for requests...");
    }
}


//wayy too long, lets make a simple html javascript page to take in username and password and sent it to the api