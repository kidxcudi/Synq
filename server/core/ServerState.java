package core;

import client.ClientConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe global server state
 * Manages users, bindings, and waiting lists
 */
public class ServerState {
    // Active user connections
    public static ConcurrentHashMap<String, ClientConnection> users;
    
    // Keyless bind waiting list: username -> target
    public static ConcurrentHashMap<String, String> waitingKeyless;
    
    // Keyed bind waiting list
    public static CopyOnWriteArrayList<KeyEntry> waitingKeyed;
    
    // Active bound pairs: username -> partner
    public static ConcurrentHashMap<String, String> activePairs;
    
    /**
     * Initializes all server state collections
     */
    public static void init() {
        users = new ConcurrentHashMap<>();
        waitingKeyless = new ConcurrentHashMap<>();
        waitingKeyed = new CopyOnWriteArrayList<>();
        activePairs = new ConcurrentHashMap<>();
        
        System.out.println("✓ Server state initialized");
    }
    
    /**
     * Shuts down server and closes all connections
     */
    public static void shutdown() {
        System.out.println("\nShutting down server...");
        
        // Close all client connections
        users.values().forEach(client -> {
            try {
                if (client.socket != null && !client.socket.isClosed()) {
                    client.socket.close();
                }
            } catch (Exception e) {
                System.err.println("Error closing client: " + e.getMessage());
            }
        });
        
        // Clear all state
        users.clear();
        waitingKeyless.clear();
        waitingKeyed.clear();
        activePairs.clear();
        
        System.out.println("✓ All connections closed");
    }
    
    /**
     * Gets current user count
     */
    public static int getUserCount() {
        return users.size();
    }
    
    /**
     * Gets current bind count
     */
    public static int getActiveBindCount() {
        return activePairs.size() / 2; // Each bind counts twice
    }
    
    /**
     * Entry for keyed bind waiting list
     */
    public static class KeyEntry {
        public final String userA;
        public final String userB;
        public final String hash;
        public final long timestamp;
        
        public KeyEntry(String userA, String userB, String hash) {
            this.userA = userA;
            this.userB = userB;
            this.hash = hash;
            this.timestamp = System.currentTimeMillis();
        }
        
        /**
         * Checks if entry has expired
         */
        public boolean isExpired(long timeoutMs) {
            return (System.currentTimeMillis() - timestamp) > timeoutMs;
        }
    }
}
