package org.ilghar.controller;

import com.fasterxml.jackson.databind.*;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.*;

import org.ilghar.Secrets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.*;
import java.util.*;


@RestController
public class LoginController {

    // this is the landing page
//    @GetMapping("/")
//    public String landingPage() {
//        return "Landing page!";
//    }

    // user is redirected to AWS Cognito Login/Signup page
    // responds with AWS login endpoint, client id, redirect uri and scope
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

    // AWS Cognito communicates with this endpoint
    // Code is received after successful login
    // code and secrets are exchanged for user token
    // responds frontend with the token
    @GetMapping("/api/callback")
    public ResponseEntity<Map<String, String>> callback(@RequestParam(name = "code", required = false) String code,
                                         @RequestParam(name = "error", required = false) String error) {
        System.out.println("/callback executed");

        if (code == null || error != null) {
            return ResponseEntity.badRequest().build();
        }

        String tokenResponse = exchangeCodeForTokens(code);
        if (tokenResponse == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.ok(Map.of("authToken",tokenResponse));
    }
    // /callback is supposed to do many things
    // caching
    // user session
    // database creation on sign up
    // fetch user data from database
    // create and send cookies.
    // finally will redirect to /home with the data to be rendered

    // logs user out
    // responds with AWS Cognito logout endpoint, client id and redirect uri
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
        headers.setLocation(URI.create(cognitoLogoutUrl));
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).build();
    }

    private String exchangeCodeForTokens(String code) {

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
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                System.err.println("Token exchange failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error while exchanging code for tokens: " + e.getMessage());
        }

        return null;
    }

    public ResponseEntity<String> createCookie(String token){
        HttpHeaders headers = new HttpHeaders();
        headers.add("Set-Cookie", "token=" + token);
        return ResponseEntity.ok().headers(headers).build();
    }

    public Map<String, String> parseToken(String tokenResponse) {
        try {
            String[] tokenParts = tokenResponse.split("\\.");
            if (tokenParts.length != 3) {
                throw new IllegalArgumentException("Invalid token format. Token must have 3 parts.");
            }

            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]));

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode payloadNode = objectMapper.readTree(payload);

            Map<String, String> payloadMap = new HashMap<>();
            payloadNode.fields().forEachRemaining(field -> {
                payloadMap.put(field.getKey(), field.getValue().asText());
            });

            return payloadMap;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to decode or parse the token payload", e);
        }
    }

    public void parseAndPrintTokens(String tokenResponse) {
        try {
            String[] tokenParts = tokenResponse.split("\\.");
            if (tokenParts.length != 3) {
                throw new IllegalArgumentException("Invalid token format. Token must have 3 parts.");
            }

            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]));

            ObjectMapper objectMapper = new ObjectMapper();
            var payloadMap = objectMapper.readTree(payload);

            System.out.println("Decoded Token Payload:");
            payloadMap.fields().forEachRemaining(field -> {
                System.out.println(field.getKey() + ": " + field.getValue());
            });

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to decode or parse the token payload", e);
        }
    }

    public static int getTTL(String exp){
        return Integer.parseInt(exp) - (int)(System.currentTimeMillis()/1000);
    }


}