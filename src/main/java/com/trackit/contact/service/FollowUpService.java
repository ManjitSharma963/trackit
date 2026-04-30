package com.trackit.contact.service;

import com.trackit.contact.model.Contact;
import com.trackit.contact.model.FollowUp;
import com.trackit.contact.repository.ContactRepository;
import com.trackit.contact.repository.FollowUpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FollowUpService {

    @Autowired
    private FollowUpRepository followUpRepo;

    @Autowired
    private ContactRepository contactRepo;

    public FollowUp create(Long contactId, FollowUp f) {
        Contact contact = contactRepo.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));

        f.setContact(contact);
        f.setStatus("PENDING");

        return followUpRepo.save(f);
    }

    public List<FollowUp> getByContact(Long contactId) {
        return followUpRepo.findByContactId(contactId);
    }

    public List<FollowUp> getToday() {
        return followUpRepo.findByFollowUpDate(LocalDate.now());
    }

    public FollowUp markDone(Long id) {
        FollowUp existing = followUpRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Follow-up not found"));
        existing.setStatus("DONE");
        return followUpRepo.save(existing);
    }

    public FollowUp update(Long id, FollowUp updated) {
        FollowUp existing = followUpRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Follow-up not found"));
        existing.setNote(updated.getNote());
        existing.setFollowUpDate(updated.getFollowUpDate());
        return followUpRepo.save(existing);
    }

    public void delete(Long id) {
        followUpRepo.deleteById(id);
    }
}
