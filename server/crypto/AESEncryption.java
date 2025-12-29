package crypto;

import config.ServerConfig;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM encryption and decryption
 * Provides authenticated encryption with random IV per message
 */
public class AESEncryption {
    private static final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Encrypts plaintext using AES-GCM
     * @param key AES secret key
     * @param plaintext Text to encrypt
     * @return Base64 encoded (IV + ciphertext)
     */
    public static String encrypt(SecretKey key, String plaintext) throws Exception {
        // Generate random IV
        byte[] iv = new byte[ServerConfig.GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        
        // Encrypt with AES-GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(ServerConfig.GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));
        
        // Prepend IV to ciphertext
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    /**
     * Decrypts ciphertext using AES-GCM
     * @param key AES secret key
     * @param base64Combined Base64 encoded (IV + ciphertext)
     * @return Decrypted plaintext
     */
    public static String decrypt(SecretKey key, String base64Combined) throws Exception {
        byte[] combined = Base64.getDecoder().decode(base64Combined);
        
        // Extract IV and ciphertext
        byte[] iv = new byte[ServerConfig.GCM_IV_LENGTH];
        byte[] ciphertext = new byte[combined.length - ServerConfig.GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);
        
        // Decrypt with AES-GCM
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(ServerConfig.GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] plaintext = cipher.doFinal(ciphertext);
        
        return new String(plaintext, "UTF-8");
    }
}
