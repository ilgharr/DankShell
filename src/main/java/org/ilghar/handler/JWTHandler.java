package org.ilghar.handler;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.ilghar.Secrets;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@RestController
public class JWTHandler {

    // Generate RSA keys, will print the key onto screen!
    public static void generateRSAKeys() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        String privateKey = Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded());
        String publicKey = Base64.getEncoder().encodeToString(pair.getPublic().getEncoded());

        System.out.println("Private Key: " + privateKey);
        System.out.println("Public Key: " + publicKey);
    }

//    public static String generateToken() throws Exception {
//        byte[] privateKeyBytes = Base64.getDecoder().decode(Secrets.MOCK_PRIVATE_KEY);
//        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
//        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
//        PrivateKey privateKey = keyFactory.generatePrivate(spec);
//
//        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
//                .issuer("http://localhost:8443")
//                .subject("test-user")
//                .audience("test-audience")
//                .expirationTime(new Date(System.currentTimeMillis() + 60000))
//                .notBeforeTime(new Date())
//                .issueTime(new Date())
//                .claim("role", "admin")
//                .build();
//
//        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
//                .keyID("test-key-id-1")
//                .type(JOSEObjectType.JWT)
//                .build();
//
//        SignedJWT signedJWT = new SignedJWT(header, claimSet);
//        JWSSigner signer = new RSASSASigner(privateKey);
//        signedJWT.sign(signer);
//        return signedJWT.serialize();
//    }

    public static boolean validateToken(String idToken, String jwks_url, String client_id) {
        try {
            // Parse the token
            SignedJWT signedJWT = SignedJWT.parse(idToken);

            // Get Cognito's JSON Web Key Set (JWKS)
            JWKSet jwkSet = JWKSet.load(new URL(jwks_url));
            JWK jwk = jwkSet.getKeyByKeyId(signedJWT.getHeader().getKeyID());

            if (jwk == null) {
                System.err.println("Invalid JWK.");
                return false;
            }

            if (!(jwk instanceof RSAKey)){
                System.err.println("Unsupported key type.");
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

            if (!claimsSet.getAudience().contains(client_id)) {
                System.err.println("Invalid audience.");
                return false;
            }

            return true;
        } catch (Exception e) {
            System.err.println("Error while validating token: " + e.getMessage());
        }

        return false;
    }

    public void sendTokenToUrl(String token, String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "text/plain"); // Token is sent as plain text

            HttpEntity<String> request = new HttpEntity<>(token, headers);
            RestTemplate restTemplate = new RestTemplate();

            // Send the POST request but *do not handle the response*
            restTemplate.postForLocation(url, request);
        } catch (Exception e) {
            // Log any errors
            e.printStackTrace();
        }
    }
}
