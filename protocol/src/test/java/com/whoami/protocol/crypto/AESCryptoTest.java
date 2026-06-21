package com.whoami.protocol.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AESCryptoTest {

    @Test
    public void testEncryptAndDecrypt() {
        AESEncrypter encrypter = new AESEncrypter();
        AESDecrypter decrypter = new AESDecrypter();

        String originalText = "Secret Character Name: Harry Potter";
        byte[] originalPayload = originalText.getBytes(StandardCharsets.UTF_8);

        byte[] encryptedPayload = encrypter.encrypt(originalPayload);
        
        // Ensure it's actually encrypted (not just the same bytes)
        assertNotEquals(originalPayload.length, encryptedPayload.length, "Encrypted payload length usually changes due to padding");
        
        byte[] decryptedPayload = decrypter.decrypt(encryptedPayload);
        
        assertArrayEquals(originalPayload, decryptedPayload, "Decrypted payload should match the original");
    }

    @Test
    public void testEmptyPayload() {
        AESEncrypter encrypter = new AESEncrypter();
        AESDecrypter decrypter = new AESDecrypter();
        
        byte[] encrypted = encrypter.encrypt(new byte[0]);
        byte[] decrypted = decrypter.decrypt(encrypted);
        
        assertArrayEquals(new byte[0], decrypted, "Empty payload should be encrypted and decrypted correctly");
    }

    @Test
    public void testDecryptInvalidDataThrowsException() {
        AESDecrypter decrypter = new AESDecrypter();
        byte[] invalidData = new byte[]{1, 2, 3, 4, 5}; // Not a multiple of AES block size
        
        assertThrows(RuntimeException.class, () -> decrypter.decrypt(invalidData), "Should throw RuntimeException on invalid AES data");
    }
}