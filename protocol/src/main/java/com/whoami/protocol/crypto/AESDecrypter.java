package com.whoami.protocol.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class AESDecrypter {
    private final SecretKey secretKey;

    private static final byte[] FIXED_KEY = "1234567890123456".getBytes(StandardCharsets.UTF_8);

    public AESDecrypter() {
        this.secretKey = new SecretKeySpec(FIXED_KEY, "AES");
    }

    public AESDecrypter(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public byte[] decrypt(byte[] encryptedPayload) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return cipher.doFinal(encryptedPayload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt payload", e);
        }
    }
}
