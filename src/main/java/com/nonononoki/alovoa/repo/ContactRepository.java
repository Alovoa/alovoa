package com.nonononoki.alovoa.repo;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nonononoki.alovoa.entity.Contact;

public interface ContactRepository extends JpaRepository<Contact, Long> {
	List<Contact> findTop20ByHiddenFalse();

	void deleteByDateBefore(Date date);
}
