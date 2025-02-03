package org.ilghar.controller;

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
    @PostMapping("/getid")
    public ResponseEntity<String> getID(@RequestBody Map<String, String> payload) {
        String userId = payload.get("userId");

        if (userId == null || userId.isEmpty()) {
            return ResponseEntity.badRequest().body("User ID is missing");
        }

        String id_token = memcached.memcachedGetData(userId);

        System.out.println(id_token);

        Map<String, String> identityResponse = fetchIdentityId(id_token);
        if (identityResponse != null) {
            System.out.println("Identity ID: " + identityResponse.get("IdentityId"));
        } else {
            System.err.println("Failed to fetch Identity ID.");
        }

        return ResponseEntity.ok("User ID received successfully");
    }


    public static Map<String, String> fetchIdentityId(String idToken) {
        RestTemplate restTemplate = new RestTemplate();

        // Headers setup
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Amz-Target", "AWSCognitoIdentityService.GetId");

        // Request body setup
        try {
            // Construct the request body
            Map<String, Object> requestBody = Map.of(
                    "IdentityPoolId", Secrets.IDENTITY_POOL_ID,
                    "Logins", Map.of("cognito-idp.ca-central-1.amazonaws.com/" + Secrets.USER_POOL_ID, idToken)
            );

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // Send the POST request
            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://cognito-identity.ca-central-1.amazonaws.com/",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            // Process the response
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseMap = response.getBody();
                if (responseMap.containsKey("IdentityId")) {
                    System.out.println("Successfully fetched Identity ID: " + responseMap.get("IdentityId"));
                    return Map.of("IdentityId", (String) responseMap.get("IdentityId"));
                } else {
                    System.err.println("Response does not contain IdentityId: " + responseMap);
                }
            } else {
                System.err.println("Failed to fetch Identity ID. Response status: " + response.getStatusCode());
                if (response.getBody() != null) {
                    System.err.println("Response body: " + response.getBody());
                }
            }
        } catch (Exception e) {
            System.err.println("Error while fetching Identity ID: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for debugging
        }

        // Return an error map if the process failed
        return Map.of("error", "Identity ID retrieval failed");
    }

//    public static Map<String, String> fetchIdentityId(String idToken) {
//        RestTemplate restTemplate = new RestTemplate();
//
//        // Headers setup
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        headers.set("X-Amz-Target", "AWSCognitoIdentityService.GetId");
//
//        // Request body setup
//        Map<String, Object> requestBody = Map.of(
//                "IdentityPoolId", Secrets.IDENTITY_POOL_ID,
//                "Logins", Map.of("cognito-idp.ca-central-1.amazonaws.com/" + Secrets.USER_POOL_ID, idToken)
//        );
//
//        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
//
//        try {
//            // Send POST request
//            ResponseEntity<String> response = restTemplate.exchange(
//                    "https://cognito-identity.ca-central-1.amazonaws.com/",
//                    HttpMethod.POST,
//                    entity,
//                    String.class
//            );
//
//            // Parse the JSON string into a Map<String, String>
//            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
//                ObjectMapper objectMapper = new ObjectMapper();
//                Map<String, String> responseMap = objectMapper.readValue(response.getBody(), Map.class);
//                return responseMap;
//            } else {
//                System.err.println("Failed to fetch Identity ID. Status code: " + response.getStatusCode());
//            }
//        } catch (Exception e) {
//            System.err.println("Error while fetching Identity ID: " + e.getMessage());
//        }
//
//        return null; // Return null on failure
//    }

}
