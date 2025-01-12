package shitshell.server;

import java.io.IOException;

/**
 * Hello world!
 *
 */
public class Main
{
    public static void main( String[] args ){
        try {
            SimpleServer.startServer();
        } catch (Exception e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
