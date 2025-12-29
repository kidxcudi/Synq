package crypto;

import config.ServerConfig;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;

/**
 * Diffie-Hellman key exchange implementation
 * Uses RFC 3526 standard parameters
 */
public class DHKeyExchange {
    // RFC 3526 - 2048-bit MODP Group 14 (standard safe prime)
    private static final BigInteger DH_P = new BigInteger(
        "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1" +
        "29024E088A67CC74020BBEA63B139B22514A08798E3404DD" +
        "EF9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245" +
        "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED" +
        "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3D" +
        "C2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F" +
        "83655D23DCA3AD961C62F356208552BB9ED529077096966D" +
        "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3B" +
        "E39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9" +
        "DE2BCBF6955817183995497CEA956AE515D2261898FA0510" +
        "15728E5A8AACAA68FFFFFFFFFFFFFFFF", 16
    );
    private static final BigInteger DH_G = BigInteger.valueOf(2);
    
    /**
     * Generates a new DH key pair using standard parameters
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DH");
        DHParameterSpec dhSpec = new DHParameterSpec(DH_P, DH_G);
        kpg.initialize(dhSpec);
        return kpg.generateKeyPair();
    }
    
    /**
     * Validates received DH public key
     * Prevents small subgroup attacks
     */
    public static boolean isValidPublicKey(PublicKey key) {
        try {
            if (!(key instanceof javax.crypto.interfaces.DHPublicKey)) {
                return false;
            }
            
            javax.crypto.interfaces.DHPublicKey dhKey = (javax.crypto.interfaces.DHPublicKey) key;
            BigInteger y = dhKey.getY();
            
            // Validate: 1 < y < p-1
            return y.compareTo(BigInteger.ONE) > 0 && 
                   y.compareTo(DH_P.subtract(BigInteger.ONE)) < 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Parses client public key from encoded bytes
     */
    public static PublicKey parsePublicKey(byte[] encodedKey) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedKey);
        return keyFactory.generatePublic(keySpec);
    }
    
    /**
     * Completes key agreement and derives AES key
     */
    public static SecretKeySpec deriveAESKey(KeyAgreement keyAgreement) throws Exception {
        byte[] sharedSecret = keyAgreement.generateSecret();
        
        // Hash shared secret to get AES key
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(sharedSecret);
        
        // Use first 128 bits for AES-128
        return new SecretKeySpec(keyBytes, 0, ServerConfig.AES_KEY_SIZE / 8, "AES");
    }
    
    /**
     * Initializes key agreement with private key
     */
    public static KeyAgreement initKeyAgreement(PrivateKey privateKey) throws Exception {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(privateKey);
        return keyAgreement;
    }
}
