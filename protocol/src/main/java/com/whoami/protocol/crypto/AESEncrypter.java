package com.whoami.protocol.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class AESEncrypter {
    private final SecretKey secretKey;

    // We can use a predefined key for simplicity in the coursework
    private static final byte[] FIXED_KEY = "1234567890123456".getBytes(StandardCharsets.UTF_8);

    public AESEncrypter() {
        this.secretKey = new SecretKeySpec(FIXED_KEY, "AES");
    }

    public AESEncrypter(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public byte[] encrypt(byte[] payload) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return cipher.doFinal(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt payload", e);
        }
    }
}
