package com.trackit.secret;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/secrets")
@PreAuthorize("hasRole('USER')")
public class SecretController {

    private final SecretService secretService;

    public SecretController(SecretService secretService) {
        this.secretService = secretService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SecretResponse create(@Valid @RequestBody SecretRequest request) {
        return secretService.create(request);
    }

    @GetMapping
    public List<SecretResponse> list() {
        return secretService.list();
    }

    @GetMapping("/{id}")
    public SecretResponse getById(@PathVariable Long id) {
        return secretService.getById(id);
    }

    @PutMapping("/{id}")
    public SecretResponse update(
            @PathVariable Long id,
            @Valid @RequestBody SecretRequest request) {
        return secretService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        secretService.delete(id);
    }
}
