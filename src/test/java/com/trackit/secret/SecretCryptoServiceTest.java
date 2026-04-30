package com.trackit.secret;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretCryptoServiceTest {

    @Test
    void shouldEncryptAndDecryptRoundTrip() {
        SecretEncryptionProperties props = new SecretEncryptionProperties();
        props.setEncryptionKeyBase64("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        SecretCryptoService cryptoService = new SecretCryptoService(props);

        String encrypted = cryptoService.encrypt("MyPass@123");
        String decrypted = cryptoService.decrypt(encrypted);

        assertThat(encrypted).startsWith("v1:");
        assertThat(encrypted).doesNotContain("MyPass@123");
        assertThat(decrypted).isEqualTo("MyPass@123");
    }
}
