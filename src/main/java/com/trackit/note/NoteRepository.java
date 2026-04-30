package com.trackit.notes.repository;

import com.trackit.notes.model.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoteRepository extends JpaRepository<Note, Long> {
    Page<Note> findByUserId(Long userId, Pageable pageable);
}
