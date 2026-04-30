package com.trackit.contact.controller;

import com.trackit.contact.model.Contact;
import com.trackit.contact.repository.ContactRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contacts")
@PreAuthorize("hasRole('USER')")
public class ContactController {

    private final ContactRepository contactRepo;

    public ContactController(ContactRepository contactRepo) {
        this.contactRepo = contactRepo;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Contact create(@RequestBody Contact contact) {
        return contactRepo.save(contact);
    }

    @GetMapping
    public List<Contact> getAll() {
        return contactRepo.findAll();
    }

    @GetMapping("/{id}")
    public Contact getById(@PathVariable Long id) {
        return contactRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        contactRepo.deleteById(id);
    }
}
