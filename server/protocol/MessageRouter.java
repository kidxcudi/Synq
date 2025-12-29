package protocol;

import client.ClientConnection;
import core.ServerState;
import crypto.CryptoUtils;
import util.JsonUtils;
import util.Validator;
import java.util.HashMap;
import java.util.Map;

/**
 * Routes chat messages between bound users
 */
public class MessageRouter {
    
    /**
     * Routes message from sender to their bound partner
     */
    public static RouteResult routeMessage(String sender, String messageText) {
        // Validate message
        if (!Validator.isValidMessage(messageText)) {
            return RouteResult.error("invalid_message");
        }
        
        // Check if sender is bound
        String partner = BindManager.getPartner(sender);
        if (partner == null) {
            return RouteResult.error("not_bound");
        }
        
        // Get partner connection
        ClientConnection partnerConn = ServerState.users.get(partner);
        if (partnerConn == null) {
            return RouteResult.error("partner_offline");
        }
        
        // Create relay message
        Map<String, String> relayMsg = new HashMap<>();
        relayMsg.put("type", "message");
        relayMsg.put("from", sender);
        relayMsg.put("text", messageText);
        
        try {
            // Encrypt and send
            String encrypted = CryptoUtils.encrypt(partnerConn.aesKey, JsonUtils.toJson(relayMsg));
            partnerConn.out.println(encrypted);
            return RouteResult.success();
        } catch (Exception e) {
            System.err.println("Failed to relay message: " + e.getMessage());
            return RouteResult.error("relay_failed");
        }
    }
    
    /**
     * Notifies partner that user disconnected
     */
    public static void notifyPartnerDisconnected(String disconnectedUser, String partner) {
        ClientConnection partnerConn = ServerState.users.get(partner);
        if (partnerConn == null || partnerConn.aesKey == null) {
            return; // Partner already gone or not secure
        }
        
        try {
            Map<String, String> msg = new HashMap<>();
            msg.put("type", "partner_disconnected");
            
            String encrypted = CryptoUtils.encrypt(partnerConn.aesKey, JsonUtils.toJson(msg));
            partnerConn.out.println(encrypted);
            
            System.out.println("✓ Notified " + partner + " of disconnect");
        } catch (Exception e) {
            System.err.println("Failed to notify partner: " + e.getMessage());
        }
    }
    
    /**
     * Sends encrypted message to specific user
     */
    public static boolean sendToUser(String username, Map<String, String> message) {
        ClientConnection conn = ServerState.users.get(username);
        if (conn == null || conn.aesKey == null) {
            return false;
        }
        
        try {
            String encrypted = CryptoUtils.encrypt(conn.aesKey, JsonUtils.toJson(message));
            conn.out.println(encrypted);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send to " + username + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Result of message routing
     */
    public static class RouteResult {
        public final boolean success;
        public final String error;
        
        private RouteResult(boolean success, String error) {
            this.success = success;
            this.error = error;
        }
        
        public static RouteResult success() {
            return new RouteResult(true, null);
        }
        
        public static RouteResult error(String error) {
            return new RouteResult(false, error);
        }
    }
}
