package org.ilghar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ilghar.Secrets;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;


@RestController
public class LoginController {

    @GetMapping("/login")
    public ResponseEntity<Void> login() throws IOException {
        String cognitoUrl = Secrets.AUTHORIZATION_ENDPOINT + "?" +
                "client_id=" + Secrets.CLIENT_ID + "&" +
                "response_type=code&" +
                "redirect_uri=" + Secrets.REDIRECT_URI + "&" +
                "scope=" + Secrets.SCOPES;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Location", cognitoUrl);

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam(name = "code", required = false) String code,
                                         @RequestParam(name = "error", required = false) String error) {
        if (code == null) {
            // If the 'code' is missing, it's a bad request
            return ResponseEntity.badRequest().build();
        }

        if (error != null) {
            // Handle OAuth2 errors (optional - return an error page or status)
            return ResponseEntity.badRequest().build();
        }

        // Exchange authorization code for tokens
        String tokenResponse = exchangeCodeForTokens(code);
        if (tokenResponse == null) {
            // If token exchange fails, return an internal server error
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        decodeTokenWithJsonPayload(tokenResponse);

        // On successful exchange, redirect the user to /home
        HttpHeaders headers = new HttpHeaders();
        return new ResponseEntity<>(headers, HttpStatus.FOUND); // HTTP 302 Found
    }

    private String exchangeCodeForTokens(String code) {
        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("client_id", Secrets.CLIENT_ID);
        requestBody.add("client_secret", Secrets.CLIENT_SECRET);
        requestBody.add("redirect_uri", Secrets.REDIRECT_URI);
        requestBody.add("code", code);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    Secrets.TOKEN_ENDPOINT, new HttpEntity<>(requestBody, headers), String.class);

            return response.getBody(); // Return the full token response (likely a JSON string)
        } catch (Exception e) {
            System.err.println("Error while exchanging code for tokens: " + e.getMessage());
            return null;
        }
    }

    public static void decodeTokenWithJsonPayload(String json) {
        try {
            // Step 1: Parse the JSON to extract the token (id_token or access_token)
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> tokenMap = objectMapper.readValue(json, Map.class); // Parse JSON as a Map
            String token = tokenMap.get("id_token"); // Extract `id_token` key (use `access_token` if needed)

            if (token == null || token.isEmpty()) {
                System.err.println("Missing or empty id_token in the JSON payload.");
                return;
            }

            // Step 2: Split the JWT into its three parts: header, payload, signature
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                System.err.println("Invalid JWT format! Ensure only the JWT is passed.");
                return;
            }

            // Step 3: Decode Header and Payload parts from Base64URL
            String header = new String(Base64.getUrlDecoder().decode(parts[0]));
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

            // Step 4: Print the decoded parts
            System.out.println("Decoded JWT Header: " + header);
            System.out.println("Decoded JWT Payload: " + payload);

        } catch (Exception e) {
            // Handle JSON parsing or decoding errors gracefully
            System.err.println("Error: " + e.getMessage());
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logout() {
        String cognitoLogoutUrl = String.format(
                "%s?client_id=%s&logout_uri=%s",
                Secrets.LOGOUT_ENDPOINT,
                Secrets.CLIENT_ID,
                Secrets.LOGOUT_URI
        );

        System.out.println("Logging out from Cognito with URL: " + cognitoLogoutUrl);

        // Set the redirection headers
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(cognitoLogoutUrl)); // Use setLocation for readability

        // Return 302 (Found) redirect
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    @GetMapping("/logout-success")
    public String logoutSuccess() {
        return "redirect:/"; // Redirects to home page
    }
}

