package protocol;

import client.ClientConnection;
import core.ServerState;
import crypto.CryptoUtils;
import util.Validator;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages user binding logic
 * Handles both keyless and security-key binding
 */
public class BindManager {
    
    /**
     * Handles keyless binding request
     * Both users must request each other
     */
    public static BindResult handleKeylessBind(String requester, String target) {
        // Validate target exists
        if (!ServerState.users.containsKey(target)) {
            return BindResult.error("target_offline");
        }
        
        // Check not binding to self
        if (requester.equals(target)) {
            return BindResult.error("cannot_bind_self");
        }
        
        // Check if already bound
        if (ServerState.activePairs.containsKey(requester)) {
            return BindResult.error("already_bound");
        }
        
        // Store this bind request
        ServerState.waitingKeyless.put(requester, target);
        
        // Check if target also wants to bind with requester
        String targetWants = ServerState.waitingKeyless.get(target);
        if (requester.equals(targetWants)) {
            // Mutual bind found!
            completeBind(requester, target);
            return BindResult.success(target);
        }
        
        return BindResult.waiting();
    }
    
    /**
     * Handles security-key binding request
     * Both users must provide matching hash
     */
    public static BindResult handleKeyedBind(String requester, String target, String hash) {
        // Validate hash format
        if (!Validator.isValidHash(hash)) {
            return BindResult.error("invalid_hash");
        }
        
        // Validate target exists
        if (!ServerState.users.containsKey(target)) {
            return BindResult.error("target_offline");
        }
        
        // Check not binding to self
        if (requester.equals(target)) {
            return BindResult.error("cannot_bind_self");
        }
        
        // Check if already bound
        if (ServerState.activePairs.containsKey(requester)) {
            return BindResult.error("already_bound");
        }
        
        // Ensure canonical ordering (alphabetical)
        String userA = requester.compareTo(target) < 0 ? requester : target;
        String userB = requester.compareTo(target) < 0 ? target : requester;
        
        // Check for matching entry
        for (ServerState.KeyEntry entry : ServerState.waitingKeyed) {
            if (entry.userA.equals(userA) && 
                entry.userB.equals(userB) && 
                CryptoUtils.constantTimeEquals(entry.hash, hash)) {
                // Match found!
                ServerState.waitingKeyed.remove(entry);
                completeBind(userA, userB);
                return BindResult.success(requester.equals(userA) ? userB : userA);
            }
        }
        
        // No match, add our entry
        ServerState.waitingKeyed.add(new ServerState.KeyEntry(userA, userB, hash));
        return BindResult.waiting();
    }
    
    /**
     * Completes binding between two users
     */
    private static void completeBind(String userA, String userB) {
        // Register active pair (bidirectional)
        ServerState.activePairs.put(userA, userB);
        ServerState.activePairs.put(userB, userA);
        
        // Clear waiting entries
        ServerState.waitingKeyless.remove(userA);
        ServerState.waitingKeyless.remove(userB);
        
        System.out.println("✓ Bind: " + userA + " <-> " + userB);
    }
    
    /**
     * Unbinds user (on disconnect)
     * Returns partner username if was bound
     */
    public static String unbindUser(String username) {
        // Remove from waiting lists
        ServerState.waitingKeyless.remove(username);
        ServerState.waitingKeyed.removeIf(e -> 
            e.userA.equals(username) || e.userB.equals(username));
        
        // Remove from active pairs
        String partner = ServerState.activePairs.remove(username);
        if (partner != null) {
            ServerState.activePairs.remove(partner);
            System.out.println("✗ Unbind: " + username + " <-> " + partner);
        }
        
        return partner;
    }
    
    /**
     * Gets partner username if bound
     */
    public static String getPartner(String username) {
        return ServerState.activePairs.get(username);
    }
    
    /**
     * Checks if user is currently bound
     */
    public static boolean isBound(String username) {
        return ServerState.activePairs.containsKey(username);
    }
    
    /**
     * Result of bind operation
     */
    public static class BindResult {
        public final boolean success;
        public final boolean waiting;
        public final String partner;
        public final String error;
        
        private BindResult(boolean success, boolean waiting, String partner, String error) {
            this.success = success;
            this.waiting = waiting;
            this.partner = partner;
            this.error = error;
        }
        
        public static BindResult success(String partner) {
            return new BindResult(true, false, partner, null);
        }
        
        public static BindResult waiting() {
            return new BindResult(false, true, null, null);
        }
        
        public static BindResult error(String error) {
            return new BindResult(false, false, null, error);
        }
        
        public Map<String, String> toMessage() {
            Map<String, String> msg = new HashMap<>();
            if (success) {
                msg.put("type", "bind_success");
                msg.put("partner", partner);
            } else if (waiting) {
                msg.put("type", "info");
                msg.put("message", "waiting_for_partner");
            } else {
                msg.put("type", "error");
                msg.put("error", error);
            }
            return msg;
        }
    }
}
