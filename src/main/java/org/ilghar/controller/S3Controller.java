package org.ilghar.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ilghar.Secrets;
import org.ilghar.handler.MemcachedHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
public class S3Controller {

    @Autowired
    public MemcachedHandler memcached;

    // test endpoint to check if I can get the correct user_id
//    @PostMapping("/getid")
//    public ResponseEntity<String> getID(@RequestBody Map<String, String> payload) throws JsonProcessingException {
//        String userId = payload.get("userId");
//
//        if (userId == null || userId.isEmpty()) {
//            return ResponseEntity.badRequest().body("User ID is missing");
//        }
//
//        String token = memcached.memcachedGetData(userId);
//        ObjectMapper objectMapper = new ObjectMapper();
//
//        Map<String, String> tokenMap = objectMapper.readValue(token, Map.class);
//
//        String idToken = tokenMap.get("id_token");
//        String accessToken = tokenMap.get("access_token");
//
//        Map<String, String> identityResponse = fetchIdentityId(idToken);
//        if (identityResponse != null) {
//            System.out.println("Identity ID: " + identityResponse.get("IdentityId"));
//        } else {
//            System.err.println("Failed to fetch Identity ID.");
//        }
//
//        Map<String, String> credentialsResponse = fetchTemporaryCredentials(idToken, accessToken);
//
////        System.out.println("Access Key: " + credentialsResponse.get("AccessKeyId"));
//
//        return ResponseEntity.ok("User ID received successfully");
//    }

@PostMapping("/getid")
public ResponseEntity<String> getID(@RequestBody Map<String, String> payload) throws JsonProcessingException {
    String userId = payload.get("userId");

    if (userId == null || userId.isEmpty()) {
        return ResponseEntity.badRequest().body("User ID is missing");
    }

    String token = memcached.memcachedGetData(userId);
    if (token == null) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No token found for the given User ID.");
    }

    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, String> tokenMap = objectMapper.readValue(token, Map.class);

    String id_token = tokenMap.get("id_token");
    String access_token = tokenMap.get("access_token");


    // Fetch Identity ID
    Map<String, String> identityResponse = fetchIdentityId(id_token);
    if (identityResponse == null || !identityResponse.containsKey("IdentityId")) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch Identity ID.");
    }

    System.out.println("Identity ID: " + identityResponse.get("IdentityId"));

    // Fetch Temporary Credentials
//    Map<String, String> credentialsResponse = fetchTemporaryCredentials(identityResponse.get("IdentityId"), id_token);
//    if (credentialsResponse == null) {
//        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to fetch temporary credentials.");
//    }

    return ResponseEntity.ok("User ID received successfully");
}

    public Map<String, String> fetchIdentityId(String id_token) {
        try {
            Map<String, Object> payload = Map.of(
                    "IdentityPoolId", Secrets.IDENTITY_POOL_ID,
                    "Logins", Map.of(
                            Secrets.USER_POOL_ID, id_token
                    )
            );

            ObjectMapper objectMapper = new ObjectMapper();
            String body = objectMapper.writeValueAsString(payload);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-amz-json-1.1");
            headers.set("X-Amz-Target", "AWSCognitoIdentityService.GetId");

            HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                    Secrets.CREDENTIALS_ENDPOINT,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );

            String responseBody = response.getBody();
            @SuppressWarnings("unchecked")
            Map<String, String> responseMap = objectMapper.readValue(responseBody, Map.class);

            return responseMap;
        } catch (Exception ex) {
            System.err.println("Failed to fetch Identity ID: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    public Map<String, String> fetchTemporaryCredentials(String identity_id, String id_token) throws JsonProcessingException {
        System.out.println("id token: " + id_token);
        try {
            // Include the Logins map for authenticated users
            Map<String, Object> payload = identity_id != null && !identity_id.isEmpty()
                    ? Map.of(
                    "IdentityId", identity_id,
                    "Logins", Map.of(
                            "cognito-idp.ca-central-1.amazonaws.com/" + Secrets.USER_POOL_ID, id_token
                    ))
                    : Map.of(
                    "Logins", Map.of(
                            "cognito-idp.ca-central-1.amazonaws.com/" + Secrets.USER_POOL_ID, id_token
                    ));

            ObjectMapper objectMapper = new ObjectMapper();
            String body = objectMapper.writeValueAsString(payload);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/x-amz-json-1.1");
            headers.set("X-Amz-Target", "AWSCognitoIdentityService.GetCredentialsForIdentity");

            HttpEntity<String> httpEntity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.exchange(
                    Secrets.CREDENTIALS_ENDPOINT,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );

            String responseBody = response.getBody();
            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);

            // Parse the response to get AWS credentials
            @SuppressWarnings("unchecked")
            Map<String, String> credentials = (Map<String, String>) responseMap.get("Credentials");
            return credentials;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
