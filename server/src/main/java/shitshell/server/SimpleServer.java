
package shitshell.server;

import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class SimpleServer {

    public static void startServer() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8443), 0);
        server.createContext("/callback", new CallbackHandler());
        server.start();
    }
}