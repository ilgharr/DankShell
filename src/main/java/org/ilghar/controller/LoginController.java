package org.ilghar.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jwt.*;

import org.ilghar.Secrets;

import org.ilghar.handler.MemcachedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.*;
import java.util.*;

import static org.ilghar.controller.LoginHelper.*;


@RestController
public class LoginController {

    @Autowired
    public MemcachedHandler memcached;

    @GetMapping("/login")
    public ResponseEntity<Void> login() throws IOException {
        // user is redirected to AWS Cognito Login/Signup page
        // responds with AWS login endpoint, client id, redirect uri and scope
        String cognitoUrl = Secrets.AUTHORIZATION_ENDPOINT + "?" +
                "client_id=" + Secrets.CLIENT_ID + "&" +
                "response_type=code&" +
                "redirect_uri=" + Secrets.REDIRECT_URI + "&" +
                "scope=" + Secrets.SCOPES;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Location", cognitoUrl);

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/api/callback")
    public ResponseEntity<Map<String, String>> callback(@RequestParam(name = "code", required = false) String code,
                                                        @RequestParam(name = "error", required = false) String error) throws JsonProcessingException {
        // AWS Cognito communicates with this endpoint
        // Code is received after successful login
        // code and secrets are exchanged for user token
        // responds frontend with the token
        if (code == null || error != null) {
            return ResponseEntity.badRequest().build();
        }

        // Exchange code for tokens
        Map<String, String> tokenResponse = exchangeCodeForTokens(code);
        if (tokenResponse == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        String user_id = extractUserId(tokenResponse);
        String access_token = extractAccessToken(tokenResponse);
        if(user_id != null && access_token != null){
            memcached.memcachedAddData(user_id, access_token, 60000);
        }


        // Respond with the Map, returned as JSON by Spring automatically
        return ResponseEntity.ok(tokenResponse);
    }

    // /callback is supposed to do many things
    // caching
    // user session
    // database creation on sign up
    // fetch user data from database
    // create and send cookies.
    // finally will redirect to /home with the data to be rendered




    @GetMapping("/logout")
    public ResponseEntity<Void> logout() {
        // logs user out
        // responds with AWS Cognito logout endpoint, client id and redirect uri
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


}