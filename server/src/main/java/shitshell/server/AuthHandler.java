package shitshell.server;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class AuthHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Print "Request received" to the console
        System.out.println("Request received");

        // This header defines which origins are allowed to access the server's resources.
        // If you wanted to restrict access to a single origin (for example, `http://localhost:8081`), you could replace `*` with the specific origin:
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

        //- This header tells the browser which **HTTP methods** (e.g., GET, POST, OPTIONS) are allowed for cross-origin requests.
        //- Including `OPTIONS` here is critical because browsers may send **preflight requests** when making certain types of requests (like POST with JSON data). A preflight request is an `OPTIONS` request sent before the actual request to check what is allowed.
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");

        //- This header specifies the **custom headers** that are allowed to be sent in the actual HTTP request.
        //- Without this, browsers block requests if they have custom headers like `Content-Type`, `Authorization`, or others.
        //- Here, you are explicitly allowing the `Content-Type` header, which is often included in POST requests when sending JSON data:
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1); // No body for OPTIONS
            return;
        }

        InputStream inputStream = exchange.getRequestBody();
        byte[] requestBodyBytes = inputStream.readAllBytes();
        String requestData = new String(requestBodyBytes);
        System.out.println("Received: " + requestData);
        inputStream.close();

        // Respond with 200 OK but no response body
        exchange.sendResponseHeaders(200, -1); // -1 indicates no response body
    }
}
