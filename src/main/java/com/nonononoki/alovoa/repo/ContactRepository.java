package com.nonononoki.alovoa.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.Contact;

public interface ContactRepository extends JpaRepository<Contact, Long> {
}

