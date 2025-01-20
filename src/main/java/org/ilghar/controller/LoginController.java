package org.ilghar.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ilghar.Secrets;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Base64;
import java.util.List;
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

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

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

//        decodeTokenWithJsonPayload(tokenResponse);
        System.out.println("Token response: " + validateToken(tokenResponse));
        return ResponseEntity.ok("Login successful!");
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

    private boolean validateToken(String token) {
        try {
            // 1. Decode the token
            SignedJWT signedJWT = SignedJWT.parse(token);

            // 2. Extract the `kid` from the header
            String kid = signedJWT.getHeader().getKeyID();

            // 3. Fetch the JWKS from AWS Cognito
            // Replace this URL with your Cognito User Pool's JWKS endpoint
            String jwksUrl = "https://cognito-idp.ca-central-1.amazonaws.com/ca-central-1_GLNVaRj0r/.well-known/jwks.json";
            JWKSet jwks = JWKSet.load(new URL(jwksUrl));

            // 4. Find the matching public key using the `kid`
            JWK jwk = jwks.getKeyByKeyId(kid);
            if (jwk == null || !(jwk instanceof RSAKey)) {
                System.out.println("Public key not found or invalid key type");
                return false;
            }

            RSAKey rsaKey = (RSAKey) jwk;

            // 5. Verify the token signature
            if (!signedJWT.verify(new com.nimbusds.jose.crypto.RSASSAVerifier(rsaKey))) {
                System.out.println("Invalid JWT signature!");
                return false;
            }

            // 6. Validate claims
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Validate issuer (`iss`)
            String expectedIssuer = "https://cognito-idp.ca-central-1.amazonaws.com/ca-central-1_GLNVaRj0r";
            if (!expectedIssuer.equals(claims.getIssuer())) {
                System.out.println("Issuer does not match!");
                return false;
            }

            // Validate audience (`aud`)
            String expectedAudience = "4lnf97fd4bs0emr40mfbriso4c";
            List<String> audiences = claims.getAudience();
            if (!audiences.contains(expectedAudience)) {
                System.out.println("Audience does not match!");
                return false;
            }

            // Check if the token is expired (`exp`)
            if (claims.getExpirationTime() == null || claims.getExpirationTime().before(new java.util.Date())) {
                System.out.println("Token has expired!");
                return false;
            }

            // Optional: Check email is verified (`email_verified`)
            if (!claims.getBooleanClaim("email_verified")) {
                System.out.println("Email is not verified!");
                return false;
            }

            // All checks passed
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("JWT validation failed: " + e.getMessage());
            return false;
        }
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