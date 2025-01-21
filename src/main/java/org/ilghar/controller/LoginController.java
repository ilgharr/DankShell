package org.ilghar.controller;

import com.fasterxml.jackson.databind.*;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.*;

import org.ilghar.Memcached;
import org.ilghar.Secrets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Date;

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

        boolean isValid = validateToken(tokenResponse);
        if (!isValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
        }

        return ResponseEntity.ok("Login successful!");
    }

    private String exchangeCodeForTokens(String code) {
        RestTemplate restTemplate = new RestTemplate();

        // Request body for token exchange
        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("grant_type", "authorization_code");
        requestBody.add("client_id", Secrets.CLIENT_ID);
        requestBody.add("client_secret", Secrets.CLIENT_SECRET);
        requestBody.add("redirect_uri", Secrets.REDIRECT_URI);
        requestBody.add("code", code);

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/x-www-form-urlencoded");

        try {
            // Call the token endpoint
            ResponseEntity<String> response = restTemplate.postForEntity(
                    Secrets.TOKEN_ENDPOINT, new HttpEntity<>(requestBody, headers), String.class);

            // Check if the response is successful
            if (response.getStatusCode().is2xxSuccessful()) {
                // Parse the response body using Jackson ObjectMapper
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode responseBody = objectMapper.readTree(response.getBody());

                // Check and extract the `id_token`
                if (responseBody.has("id_token")) {
                    return responseBody.get("id_token").asText();
                } else {
                    System.err.println("`id_token` not found in the token response.");
                }
            } else {
                System.err.println("Token exchange failed with status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            System.err.println("Error while exchanging code for tokens: " + e.getMessage());
        }

        return null;
    }

    private boolean validateToken(String idToken) {
        try {
            // Parse the token
            SignedJWT signedJWT = SignedJWT.parse(idToken);

            // Get Cognito's JSON Web Key Set (JWKS)
            JWKSet jwkSet = JWKSet.load(new URL(Secrets.JWKS_URL));
            JWK jwk = jwkSet.getKeyByKeyId(signedJWT.getHeader().getKeyID());

            if (jwk == null || !(jwk instanceof RSAKey)) {
                System.err.println("Invalid JWK or unsupported key type.");
                return false;
            }

            RSAKey rsaKey = (RSAKey) jwk;

            // Verify the token's signature
            RSASSAVerifier verifier = new RSASSAVerifier(rsaKey);
            if (!signedJWT.verify(verifier)) {
                System.err.println("Token signature verification failed.");
                return false;
            }

            // Validate claims (e.g., expiration, audience, etc.)
            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            if (claimsSet.getExpirationTime().before(new Date())) {
                System.err.println("Token is expired.");
                return false;
            }

            if (!claimsSet.getAudience().contains(Secrets.CLIENT_ID)) {
                System.err.println("Invalid audience.");
                return false;
            }

            return true; // Token is valid
        } catch (Exception e) {
            System.err.println("Error while validating token: " + e.getMessage());
        }

        return false; // Token validation failed
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