package com.trackit.secret;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Encryption key source for secrets-at-rest.
 * Prefer environment variable or external vault injection.
 */
@Component
@ConfigurationProperties(prefix = "app.secret")
public class SecretEncryptionProperties {

    /**
     * Base64-encoded AES key material (must decode to 16/24/32 bytes).
     * Recommended source: env var SECRET_ENCRYPTION_KEY_BASE64.
     */
    private String encryptionKeyBase64;

    /**
     * Optional metadata for ops/docs; actual vault fetching is typically handled by
     * platform-side secret injection into env/config.
     */
    private String vaultKeyRef;

    public String getEncryptionKeyBase64() {
        return encryptionKeyBase64;
    }

    public void setEncryptionKeyBase64(String encryptionKeyBase64) {
        this.encryptionKeyBase64 = encryptionKeyBase64;
    }

    public String getVaultKeyRef() {
        return vaultKeyRef;
    }

    public void setVaultKeyRef(String vaultKeyRef) {
        this.vaultKeyRef = vaultKeyRef;
    }
}

