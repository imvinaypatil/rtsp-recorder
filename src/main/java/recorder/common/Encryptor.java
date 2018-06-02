package recorder.common;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;


public class Encryptor {

    private final static int ENCRYPTOR_MIN_LENGTH = 20;
    private Cipher deCipher;
    private Cipher enCipher;
    private SecretKeySpec key;
    private IvParameterSpec ivSpec;

    public Encryptor(String encryptionKey) {
        if (encryptionKey == null) {
            throw new NullPointerException();
        }
        if (encryptionKey.length() < ENCRYPTOR_MIN_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("Length of encryptorKey should be at least %s characters.", ENCRYPTOR_MIN_LENGTH)
            );
        }

        byte[] encryptorKeyBytes = encryptionKey.getBytes();
        byte[] ivBytes = Arrays.copyOfRange(encryptorKeyBytes, 0, 8);
        byte[] keyBytes = Arrays.copyOfRange(encryptorKeyBytes, 8, encryptorKeyBytes.length);
        // wrap key data in Key/IV specs to pass to cipher

        ivSpec = new IvParameterSpec(ivBytes);
        // create the cipher with the algorithm you choose
        // see javadoc for Cipher class for more info, e.g.
        try {
            DESKeySpec dkey = new DESKeySpec(keyBytes);
            key = new SecretKeySpec(dkey.getKey(), "DES");
            enCipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            enCipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
            deCipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            deCipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
            
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException e) {
            throw new RuntimeException("Can't initialize encryptor.", e);
        }

    }

    public String encryptString(String str) {
        return Base64.getEncoder().encodeToString(encryptObject(str));
    }
    
    public byte[] encryptObject(Object obj) {
        byte[] input = BytesConverter.objectToBytes(obj);
        return encrypt(input);
    }
    
    /**
     * Synchronized because of Cipher.
     * @param input
     * @return 
     */
    public synchronized byte[] encrypt(byte[] input) {
        try {
            return enCipher.doFinal(input);
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException("Error during encryption.", ex);
        }
    }
    
    public String decryptString(String str) {
        try {
            return (String)decryptObject(Base64.getDecoder().decode(str));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Can't decode string as Base64.", e);
        }
    }

    public Object decryptObject(byte[] encrypted) {
        return BytesConverter.objectFromBytes(decrypt(encrypted));
    }
    
    /**
     * Synchronized because of Cipher.
     * @param encrypted
     * @return 
     */
    public synchronized byte[] decrypt(byte[] encrypted) {
        try {
            return deCipher.doFinal(encrypted);
        } catch (IllegalBlockSizeException | BadPaddingException ex) {
            throw new RuntimeException("Error during decryption.", ex);
        }
    }
}
