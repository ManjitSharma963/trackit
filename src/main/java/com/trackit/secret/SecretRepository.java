package com.trackit.secret;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecretRepository extends JpaRepository<Secret, Long> {
    List<Secret> findByUserIdOrderByIdDesc(Long userId);
}
