package protocol;

import client.ClientConnection;
import client.ClientSession;
import crypto.CryptoUtils;
import util.JsonUtils;
import util.Validator;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles incoming messages from clients
 * Routes to appropriate handler based on message type
 */
public class MessageHandler {
    private final ClientConnection client;
    private final ClientSession session;
    
    public MessageHandler(ClientConnection client, ClientSession session) {
        this.client = client;
        this.session = session;
    }
    
    /**
     * Processes encrypted message from client
     */
    public void handleEncryptedMessage(String encryptedLine) {
        try {
            // Decrypt message
            String decryptedJson = CryptoUtils.decrypt(client.aesKey, encryptedLine);
            JsonObject message = JsonUtils.parse(decryptedJson);
            
            // Validate message has type
            if (!JsonUtils.hasField(message, "type")) {
                sendError("missing_type");
                return;
            }
            
            String type = message.get("type").getAsString();
            
            // Route to appropriate handler
            switch (type) {
                case "bind_request":
                    handleBindRequest(message);
                    break;
                case "message":
                    handleChatMessage(message);
                    break;
                default:
                    sendError("unknown_message_type");
            }
            
        } catch (JsonSyntaxException e) {
            System.err.println("Invalid JSON from " + client.username + ": " + e.getMessage());
            sendError("invalid_json");
        } catch (Exception e) {
            System.err.println("Message processing error for " + client.username + ": " + e.getMessage());
            sendError("processing_error");
        }
    }
    
    /**
     * Handles bind request
     */
    private void handleBindRequest(JsonObject message) {
        // Validate required fields
        if (!JsonUtils.hasRequiredFields(message, "mode", "target")) {
            sendError("invalid_bind_request");
            return;
        }
        
        String mode = message.get("mode").getAsString();
        String target = Validator.sanitizeUsername(message.get("target").getAsString());
        
        BindManager.BindResult result;
        
        if ("keyless".equals(mode)) {
            result = BindManager.handleKeylessBind(client.username, target);
        } else if ("keyed".equals(mode)) {
            if (!JsonUtils.hasField(message, "hash")) {
                sendError("missing_hash");
                return;
            }
            String hash = message.get("hash").getAsString();
            result = BindManager.handleKeyedBind(client.username, target, hash);
        } else {
            sendError("invalid_bind_mode");
            return;
        }
        
        // Send result to requester
        sendMessage(result.toMessage());
        
        // If bind succeeded, also notify partner
        if (result.success) {
            MessageRouter.sendToUser(result.partner, result.toMessage());
        }
    }
    
    /**
     * Handles chat message
     */
    private void handleChatMessage(JsonObject message) {
        // Validate text field
        if (!JsonUtils.hasField(message, "text")) {
            sendError("missing_text");
            return;
        }
        
        String text = message.get("text").getAsString();
        
        // Route message
        MessageRouter.RouteResult result = MessageRouter.routeMessage(client.username, text);
        
        // Send error if routing failed
        if (!result.success) {
            sendError(result.error);
        }
    }
    
    /**
     * Sends error message to client
     */
    private void sendError(String error) {
        Map<String, String> msg = new HashMap<>();
        msg.put("type", "error");
        msg.put("error", error);
        sendMessage(msg);
    }
    
    /**
     * Sends message to client (encrypted)
     */
    private void sendMessage(Map<String, String> message) {
        try {
            session.sendEncrypted(JsonUtils.toJson(message));
        } catch (Exception e) {
            System.err.println("Failed to send message to " + client.username + ": " + e.getMessage());
        }
    }
}
