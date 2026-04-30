package com.trackit.contact.repository;

import com.trackit.contact.model.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    List<FollowUp> findByContactId(Long contactId);

    List<FollowUp> findByFollowUpDate(LocalDate date);
}
