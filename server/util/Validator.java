package util;

import config.ServerConfig;
import java.util.regex.Pattern;

/**
 * Input validation utility
 * Validates usernames, messages, and security hashes
 */
public class Validator {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern HASH_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");
    
    /**
     * Validates username format
     * Must be 3-20 alphanumeric characters or underscore
     */
    public static boolean isValidUsername(String username) {
        return username != null && 
               USERNAME_PATTERN.matcher(username).matches() &&
               username.length() >= ServerConfig.MIN_USERNAME_LENGTH &&
               username.length() <= ServerConfig.MAX_USERNAME_LENGTH;
    }
    
    /**
     * Validates chat message
     * Must be non-empty and under max length
     */
    public static boolean isValidMessage(String message) {
        return message != null && 
               message.length() > 0 && 
               message.length() <= ServerConfig.MAX_MESSAGE_LENGTH;
    }
    
    /**
     * Validates SHA-256 hash format
     * Must be 64 hex characters
     */
    public static boolean isValidHash(String hash) {
        return hash != null && HASH_PATTERN.matcher(hash).matches();
    }
    
    /**
     * Sanitizes username by trimming whitespace
     */
    public static String sanitizeUsername(String username) {
        if (username == null) return null;
        return username.trim();
    }
    
    /**
     * Validates JSON size to prevent DoS
     */
    public static boolean isValidJsonSize(String json) {
        return json != null && json.length() <= ServerConfig.MAX_JSON_SIZE;
    }
}
