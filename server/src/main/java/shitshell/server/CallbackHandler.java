package shitshell.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CallbackHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        if ("GET".equalsIgnoreCase(method)) {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> queryParams = parseQuery(query);
            if (queryParams.containsKey("error")) {
                sendJson(exchange, 400, "{\"error\": \"" + queryParams.get("error") + "\"}");
            } else if (queryParams.containsKey("code")) {
                sendJson(exchange, 200, "{\"message\": \"Authorization code received.\", \"code\": \"" + queryParams.get("code") + "\"}");
            } else {
                sendJson(exchange, 400, "{\"error\": \"No authorization code or error provided.\"}");
            }
            return;
        }

        sendJson(exchange, 405, "{\"error\": \"Only GET and OPTIONS methods are supported.\"}");
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> queryParams = new HashMap<>();
        if (query != null) {
            for (String pair : query.split("&")) {
                String[] keyValue = pair.split("=", 2);
                queryParams.put(keyValue[0], keyValue.length > 1 ? keyValue[1] : "");
            }
        }
        return queryParams;
    }

    private void sendJson(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        exchange.getResponseBody().write(response.getBytes());
        exchange.getResponseBody().close();
    }
}
