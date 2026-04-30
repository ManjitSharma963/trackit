package com.trackit.notes.service;

import com.trackit.notes.dto.*;
import com.trackit.notes.model.*;
import com.trackit.notes.repository.*;

import com.trackit.auth.service.CurrentUserProvider;
import com.trackit.common.PageResponse;
import com.trackit.common.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final CurrentUserProvider currentUserProvider;

    public NoteService(NoteRepository noteRepository, CurrentUserProvider currentUserProvider) {
        this.noteRepository = noteRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public NoteResponse create(NoteRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        Note note = new Note();
        applyRequest(note, request, userId);
        return toResponse(noteRepository.save(note));
    }

    public PageResponse<NoteResponse> list(int page, int size) {
        Long userId = currentUserProvider.getCurrentUserId();
        Page<Note> result = noteRepository.findByUserId(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    public NoteResponse getById(Long id) {
        return toResponse(findOwnedOrThrow(id));
    }

    public NoteResponse update(Long id, NoteRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        Note note = findOwnedOrThrow(id);
        applyRequest(note, request, userId);
        return toResponse(noteRepository.save(note));
    }

    public void delete(Long id) {
        noteRepository.delete(findOwnedOrThrow(id));
    }

    private Note findOwnedOrThrow(Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        Note note = noteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + id));
        if (!note.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Note not found with id: " + id);
        }
        return note;
    }

    private void applyRequest(Note note, NoteRequest request, Long userId) {
        note.setUserId(userId);
        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
    }

    private NoteResponse toResponse(Note note) {
        NoteResponse response = new NoteResponse();
        response.setId(note.getId());
        response.setUserId(note.getUserId());
        response.setTitle(note.getTitle());
        response.setContent(note.getContent());
        response.setCreatedAt(note.getCreatedAt());
        response.setUpdatedAt(note.getUpdatedAt());
        return response;
    }
}
