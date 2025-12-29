package crypto;

import javax.crypto.SecretKey;

/**
 * High-level cryptographic utilities
 * Convenience wrapper for AES and DH operations
 */
public class CryptoUtils {
    
    /**
     * Encrypts plaintext using AES-GCM
     */
    public static String encrypt(SecretKey key, String plaintext) throws Exception {
        return AESEncryption.encrypt(key, plaintext);
    }
    
    /**
     * Decrypts ciphertext using AES-GCM
     */
    public static String decrypt(SecretKey key, String ciphertext) throws Exception {
        return AESEncryption.decrypt(key, ciphertext);
    }
    
    /**
     * Constant-time string comparison
     * Prevents timing attacks on hash comparison
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
