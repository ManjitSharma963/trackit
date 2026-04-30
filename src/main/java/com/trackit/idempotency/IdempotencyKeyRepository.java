package com.trackit.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByUserIdAndIdempotencyKeyAndHttpMethodAndRequestPath(
            Long userId,
            String idempotencyKey,
            String httpMethod,
            String requestPath);
}
