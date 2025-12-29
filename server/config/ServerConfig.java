package config;

/**
 * Centralized server configuration
 * All constants and settings in one place
 */
public class ServerConfig {
    // Server settings
    public static final int PORT = 12345;
    public static final int MAX_CLIENTS = 10;
    
    // Security settings
    public static final int DH_KEY_SIZE = 2048;
    public static final int AES_KEY_SIZE = 128;
    public static final int GCM_IV_LENGTH = 12;
    public static final int GCM_TAG_LENGTH = 128;
    
    // Validation limits
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MAX_MESSAGE_LENGTH = 5000;
    public static final int MAX_JSON_SIZE = 10000;
    
    // Timeouts
    public static final int SOCKET_TIMEOUT_MS = 30000;
    public static final long BIND_TIMEOUT_MS = 60000;
    
    // Logging
    public static final boolean DEBUG_MODE = true;
    
    private ServerConfig() {
        // Prevent instantiation
    }
}
