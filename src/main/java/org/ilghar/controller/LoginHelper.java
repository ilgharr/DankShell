package org.ilghar.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ilghar.Secrets;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class LoginHelper {

    public static Map<String, String> exchangeCodeForTokens(String code) {

        // creates an object to perform HTTP request
        RestTemplate restTemplate = new RestTemplate();

        // creates a "map" to hold the form data for the HTTP request
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();

        // add keys and values inside the form
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("client_id", Secrets.CLIENT_ID);
        requestBody.add("client_secret", Secrets.CLIENT_SECRET);
        requestBody.add("redirect_uri", Secrets.REDIRECT_URI);
        requestBody.add("code", code);

        // create HTTP headers for the request
        HttpHeaders headers = new HttpHeaders();

        // sets the content type of the headers
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        try {
            // sends the POST request to the given endpoint
            ResponseEntity<String> response = restTemplate.postForEntity(
                    Secrets.TOKEN_ENDPOINT, new HttpEntity<>(requestBody, headers), String.class);

            // Check if the response is successful
            if (response.getStatusCode().is2xxSuccessful()) {
                // Parse the JSON string into a Map
                Map<String, String> responseMap = new HashMap<>();

                String[] pairs = response.getBody().replaceAll("[{}\"]", "").split(",");
                for (String pair : pairs) {
                    String[] keyValue = pair.split(":");
                    if (keyValue.length == 2) {
                        responseMap.put(keyValue[0].trim(), keyValue[1].trim());
                    }
                }

                return responseMap; // Return as a simple Map
            } else {
                System.err.println("Token exchange failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error while exchanging code for tokens: " + e.getMessage());
        }

        return null;
    }

    public static String extractUserId(Map<String, String> tokenResponse) throws JsonProcessingException {
        try{
            String idToken = tokenResponse.get("id_token");
            if(idToken == null || idToken.isEmpty()){
                throw new IllegalArgumentException("id_token is missing or invalid");
            }

            String[] tokenParts = idToken.split("\\.");
            if (tokenParts.length != 3) {
                throw new IllegalArgumentException("Invalid id_token format");
            }

            String payload = new String(Base64.getDecoder().decode(tokenParts[1]));

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode payloadJson = objectMapper.readTree(payload);

            if (payloadJson.has("sub")) {
                return payloadJson.get("sub").asText(); // Return the user ID
            } else {
                throw new IllegalArgumentException("sub claim not found in id_token");
            }
        } catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static String extractAccessToken(Map<String, String> tokenResponse) {
        try {
            String accessToken = tokenResponse.get("access_token");
            if (accessToken == null || accessToken.isEmpty()) {
                throw new IllegalArgumentException("access_token is missing or empty");
            }
            return accessToken;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String extractIdToken(Map<String, String> tokenResponse) {
        try {
            String idToken = tokenResponse.get("id_token");
            if (idToken == null || idToken.isEmpty()) {
                throw new IllegalArgumentException("id_token is missing or empty");
            }
            return idToken;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void parseAndPrintFirstJwt(Map<String, String> tokenResponse) {
        try {
            // Look for the first valid JWT token in the map
            String jwt = null;
            for (String value : tokenResponse.values()) {
                if (isJwt(value)) {
                    jwt = value;
                    break;
                }
            }

            if (jwt == null) {
                throw new IllegalArgumentException("No valid JWT found in the token response.");
            }

            // Split the JWT into its parts
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid JWT format.");
            }

            // Decode the JWT parts
            String header = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String signature = parts[2];

            // Print the JWT contents
            System.out.println("----- HEADER -----");
            prettyPrintJson(header);

            System.out.println("\n----- PAYLOAD -----");
            prettyPrintJson(payload);

            System.out.println("\n----- SIGNATURE -----");
            System.out.println(signature);

        } catch (Exception e) {
            System.err.println("Error decoding JWT: " + e.getMessage());
        }
    }

    private static boolean isJwt(String token) {
        return token != null && token.split("\\.").length == 3;
    }

    private static void prettyPrintJson(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Object jsonObject = objectMapper.readValue(json, Object.class);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
            System.out.println(prettyJson);
        } catch (Exception e) {
            System.out.println("Raw JSON: " + json); // Fallback for malformed JSON
        }
    }

}

