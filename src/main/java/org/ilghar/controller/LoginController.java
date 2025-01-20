package org.ilghar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ilghar.Secrets;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

import org.ilghar.Memcached;

@RestController
public class LoginController {

    @Autowired
    private Memcached memcached;

    @GetMapping("/")
    public String landingPage() {
        return "Landing page!"; // Redirects to home page
    }

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
    public ResponseEntity<String> callback(@RequestParam(name = "code", required = false) String code,
                                         @RequestParam(name = "error", required = false) String error) {
        if (code == null) {
            return ResponseEntity.badRequest().build();
        }

        if (error != null) {
            return ResponseEntity.badRequest().build();
        }

        String tokenResponse = exchangeCodeForTokens(code);
        if (tokenResponse == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        decodeTokenWithJsonPayload(tokenResponse);


//        HttpHeaders headers = new HttpHeaders();
//        headers.setLocation(URI.create("/home"));
//        return new ResponseEntity<>(headers, HttpStatus.FOUND); // HTTP 302 Found

        return ResponseEntity.ok("Login successful!");
    }

    public static void decodeTokenWithJsonPayload(String json) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> tokenMap = objectMapper.readValue(json, Map.class);
            String token = tokenMap.get("id_token");

            if (token == null || token.isEmpty()) {
                System.err.println("Missing or empty id_token in the JSON payload.");
                return;
            }

            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                System.err.println("Invalid JWT format! Ensure only the JWT is passed.");
                return;
            }

            String header = new String(Base64.getUrlDecoder().decode(parts[0]));
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

            System.out.println("Decoded JWT Header: " + header);
            System.out.println("Decoded JWT Payload: " + payload);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
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

            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error while exchanging code for tokens: " + e.getMessage());
            return null;
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

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(cognitoLogoutUrl)); // Use setLocation for readability
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    @GetMapping("/logout-success")
    public String logoutSuccess() {
        return "Logout successful!";
    }
}