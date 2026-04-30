package com.trackit.secret;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Reusable AES/GCM helper. Payload format: {@code v1:<base64(iv+ciphertext+tag)>}.
 */
public class AesEncryptionUtil {

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final String PAYLOAD_PREFIX = "v1:";

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesEncryptionUtil(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public String encrypt(String plainText) throws GeneralSecurityException {
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] payload = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(cipherText, 0, payload, iv.length, cipherText.length);

        return PAYLOAD_PREFIX + Base64.getEncoder().encodeToString(payload);
    }

    public String decrypt(String encryptedValue) throws GeneralSecurityException {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            throw new GeneralSecurityException("Encrypted payload is empty.");
        }
        if (!encryptedValue.startsWith(PAYLOAD_PREFIX)) {
            throw new GeneralSecurityException("Unsupported payload format.");
        }
        String body = encryptedValue.substring(PAYLOAD_PREFIX.length());
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(body);
        } catch (IllegalArgumentException ex) {
            throw new GeneralSecurityException("Encrypted payload is corrupted.", ex);
        }
        if (payload.length <= GCM_IV_LENGTH_BYTES) {
            throw new GeneralSecurityException("Encrypted payload is corrupted.");
        }

        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        byte[] cipherText = new byte[payload.length - GCM_IV_LENGTH_BYTES];
        System.arraycopy(payload, 0, iv, 0, GCM_IV_LENGTH_BYTES);
        System.arraycopy(payload, GCM_IV_LENGTH_BYTES, cipherText, 0, cipherText.length);

        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] plain = cipher.doFinal(cipherText);
        return new String(plain, StandardCharsets.UTF_8);
    }
}

