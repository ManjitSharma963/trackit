package com.trackit.contact.controller;

import com.trackit.contact.model.FollowUp;
import com.trackit.contact.service.FollowUpService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/followups")
@PreAuthorize("hasRole('USER')")
public class FollowUpController {

    private final FollowUpService service;

    public FollowUpController(FollowUpService service) {
        this.service = service;
    }

    @PostMapping
    public FollowUp create(
            @RequestParam Long contactId,
            @RequestBody FollowUp f) {
        return service.create(contactId, f);
    }

    @GetMapping("/contact/{contactId}")
    public List<FollowUp> getByContact(@PathVariable Long contactId) {
        return service.getByContact(contactId);
    }

    @GetMapping("/today")
    public List<FollowUp> getToday() {
        return service.getToday();
    }

    @PutMapping("/{id}/done")
    public FollowUp markDone(@PathVariable Long id) {
        return service.markDone(id);
    }

    @PutMapping("/{id}")
    public FollowUp update(
            @PathVariable Long id,
            @RequestBody FollowUp updated) {
        return service.update(id, updated);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
