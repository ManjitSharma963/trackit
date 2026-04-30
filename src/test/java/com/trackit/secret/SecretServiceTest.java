package com.trackit.secret;

import com.trackit.auth.service.CurrentUserProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecretServiceTest {

    @Mock
    private SecretRepository secretRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private SecretCryptoService secretCryptoService;

    @InjectMocks
    private SecretService secretService;

    @Test
    void createShouldEncodePasswordAndMaskResponse() {
        SecretRequest request = new SecretRequest();
        request.setTitle("Gmail");
        request.setUsername("manjit@gmail.com");
        request.setPassword("MyPass@123");
        request.setNotes("personal");

        Secret saved = new Secret();
        saved.setId(9L);
        saved.setUserId(1L);
        saved.setTitle("Gmail");
        saved.setUsername("manjit@gmail.com");
        saved.setPasswordEncrypted("v1:encryptedPayload");
        saved.setNotes("personal");

        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);
        when(secretCryptoService.encrypt("MyPass@123")).thenReturn("v1:encryptedPayload");
        when(secretRepository.save(any(Secret.class))).thenReturn(saved);

        SecretResponse response = secretService.create(request);

        ArgumentCaptor<Secret> captor = ArgumentCaptor.forClass(Secret.class);
        verify(secretRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordEncrypted()).isNotEqualTo("MyPass@123");
        assertThat(response.getPasswordMasked()).isEqualTo("********");
    }
}
