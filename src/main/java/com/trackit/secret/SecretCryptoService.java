package com.trackit.secret;

import com.trackit.auth.security.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Base64;

@Component
public class SecretCryptoService {
    private static final Logger log = LoggerFactory.getLogger(SecretCryptoService.class);
    private final AesEncryptionUtil aes;
    private final boolean keyConfigured;

    public SecretCryptoService(SecretEncryptionProperties props) {
        String keyBase64 = props.getEncryptionKeyBase64();
        if (keyBase64 == null || keyBase64.isBlank()) {
            this.aes = null;
            this.keyConfigured = false;
            log.warn("Secret encryption key is not configured. Secret APIs that encrypt/decrypt data will fail until SECRET_ENCRYPTION_KEY_BASE64 is set.");
            return;
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(keyBase64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("app.secret.encryption-key-base64 must be valid Base64.", ex);
        }
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalStateException("app.secret.encryption-key-base64 must decode to 16, 24, or 32 bytes.");
        }
        SecretKey secretKey = new SecretKeySpec(keyBytes, "AES");
        this.aes = new AesEncryptionUtil(secretKey);
        this.keyConfigured = true;
    }

    private void ensureKeyConfigured() {
        if (!keyConfigured) {
            throw new IllegalStateException(
                    "Missing app.secret.encryption-key-base64. Set SECRET_ENCRYPTION_KEY_BASE64 via env or vault injection.");
        }
    }

    public String encrypt(String plainText) {
        ensureKeyConfigured();
        try {
            return aes.encrypt(plainText);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to encrypt secret.", ex);
        }
    }

    public String decrypt(String encryptedValue) {
        ensureKeyConfigured();
        try {
            return aes.decrypt(encryptedValue);
        } catch (GeneralSecurityException ex) {
            throw new UnauthorizedException("Secret payload decryption failed.");
        }
    }
}
