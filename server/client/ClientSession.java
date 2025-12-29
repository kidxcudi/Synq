package client;

import config.ServerConfig;
import core.ServerState;
import crypto.CryptoUtils;
import crypto.DHKeyExchange;
import util.JsonUtils;
import util.Validator;
import com.google.gson.JsonObject;
import javax.crypto.KeyAgreement;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages client lifecycle phases
 * Handles login and key exchange
 */
public class ClientSession {
    private final ClientConnection client;
    
    public ClientSession(ClientConnection client) {
        this.client = client;
    }
    
    /**
     * Handles login phase
     * Validates and registers username
     */
    public void performLogin() throws Exception {
        String line = client.in.readLine();
        if (line == null) {
            throw new Exception("Client disconnected during login");
        }
        
        JsonObject loginMsg = JsonUtils.parse(line);
        
        // Validate login message
        if (!JsonUtils.hasRequiredFields(loginMsg, "type", "username")) {
            sendPlainResponse("error", "invalid_login_request");
            throw new Exception("Invalid login request");
        }
        
        if (!loginMsg.get("type").getAsString().equals("login")) {
            sendPlainResponse("error", "invalid_request_type");
            throw new Exception("Expected login message");
        }
        
        // Validate username
        String username = Validator.sanitizeUsername(loginMsg.get("username").getAsString());
        
        if (!Validator.isValidUsername(username)) {
            sendPlainResponse("error", "invalid_username");
            throw new Exception("Invalid username format: " + username);
        }
        
        // Atomic check and register
        if (ServerState.users.putIfAbsent(username, client) != null) {
            sendPlainResponse("error", "username_taken");
            throw new Exception("Username already taken: " + username);
        }
        
        client.username = username;
        sendPlainResponse("success", "login_success");
        
        System.out.println("✓ Login: " + username);
    }
    
    /**
     * Handles Diffie-Hellman key exchange
     * Establishes secure channel with AES-GCM
     */
    public void performKeyExchange() throws Exception {
        // Generate server DH keypair
        KeyPair serverKeyPair = DHKeyExchange.generateKeyPair();
        KeyAgreement keyAgreement = DHKeyExchange.initKeyAgreement(serverKeyPair.getPrivate());
        
        // Send server public key (Base64 encoded)
        byte[] serverPubBytes = serverKeyPair.getPublic().getEncoded();
        client.out.println(Base64.getEncoder().encodeToString(serverPubBytes));
        
        // Receive client public key
        String clientPubStr = client.in.readLine();
        if (clientPubStr == null) {
            throw new Exception("Client disconnected during key exchange");
        }
        
        byte[] clientPubBytes = Base64.getDecoder().decode(clientPubStr);
        PublicKey clientPublicKey = DHKeyExchange.parsePublicKey(clientPubBytes);
        
        // Validate client public key
        if (!DHKeyExchange.isValidPublicKey(clientPublicKey)) {
            throw new SecurityException("Invalid DH public key from client");
        }
        
        // Complete key agreement
        keyAgreement.doPhase(clientPublicKey, true);
        client.aesKey = DHKeyExchange.deriveAESKey(keyAgreement);
        
        System.out.println("✓ Secure channel: " + client.username);
    }
    
    /**
     * Sends encrypted message to client
     */
    public void sendEncrypted(String json) throws Exception {
        if (!client.isSecure()) {
            throw new IllegalStateException("Cannot send encrypted - no AES key");
        }
        String encrypted = CryptoUtils.encrypt(client.aesKey, json);
        client.out.println(encrypted);
    }
    
    /**
     * Sends plain JSON response (before encryption established)
     */
    private void sendPlainResponse(String type, String message) {
        Map<String, String> response = new HashMap<>();
        response.put("type", type);
        response.put("message", message);
        client.out.println(JsonUtils.toJson(response));
    }
}
