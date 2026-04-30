package com.trackit.secret;

import com.trackit.auth.service.CurrentUserProvider;
import com.trackit.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SecretService {

    private final SecretRepository secretRepository;
    private final CurrentUserProvider currentUserProvider;
    private final SecretCryptoService secretCryptoService;

    public SecretService(
            SecretRepository secretRepository,
            CurrentUserProvider currentUserProvider,
            SecretCryptoService secretCryptoService) {
        this.secretRepository = secretRepository;
        this.currentUserProvider = currentUserProvider;
        this.secretCryptoService = secretCryptoService;
    }

    public SecretResponse create(SecretRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        Secret secret = new Secret();
        applyRequest(secret, request, userId);
        return toResponse(secretRepository.save(secret));
    }

    public List<SecretResponse> list() {
        Long userId = currentUserProvider.getCurrentUserId();
        return secretRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SecretResponse getById(Long id) {
        return toResponse(findOwnedOrThrow(id));
    }

    public SecretResponse update(Long id, SecretRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        Secret secret = findOwnedOrThrow(id);
        applyRequest(secret, request, userId);
        return toResponse(secretRepository.save(secret));
    }

    public void delete(Long id) {
        secretRepository.delete(findOwnedOrThrow(id));
    }

    private Secret findOwnedOrThrow(Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        Secret secret = secretRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Secret not found with id: " + id));
        if (!secret.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Secret not found with id: " + id);
        }
        return secret;
    }

    private void applyRequest(Secret secret, SecretRequest request, Long userId) {
        secret.setUserId(userId);
        secret.setTitle(request.getTitle());
        secret.setUsername(request.getUsername());
        secret.setPasswordEncrypted(secretCryptoService.encrypt(request.getPassword()));
        secret.setNotes(request.getNotes());
    }

    private SecretResponse toResponse(Secret secret) {
        SecretResponse response = new SecretResponse();
        response.setId(secret.getId());
        response.setUserId(secret.getUserId());
        response.setTitle(secret.getTitle());
        response.setUsername(secret.getUsername());
        response.setPasswordMasked("********");
        response.setNotes(secret.getNotes());
        response.setCreatedAt(secret.getCreatedAt());
        return response;
    }
}
