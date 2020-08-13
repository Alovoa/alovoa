package com.nonononoki.alovoa.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nonononoki.alovoa.entity.Contact;
import com.nonononoki.alovoa.model.MailDto;
import com.nonononoki.alovoa.repo.ContactRepository;
import com.nonononoki.alovoa.service.AdminService;
import com.nonononoki.alovoa.service.AuthService;

@RestController
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private AdminService adminService;
	
	@Autowired
	private AuthService authService;
	
	@Autowired
	private ContactRepository contactRepository;
	
	
	@PostMapping("/contact/hide/{id}")
	public void hideContact(@PathVariable long id) throws Exception {	
		if(!authService.getCurrentUser().isAdmin()) {
			throw new Exception("");
		}
		Contact contact = contactRepository.findById(id).orElse(null);
		contact.setHidden(true);
		contactRepository.saveAndFlush(contact);
		
	}
	
	@PostMapping("/mail/single")
	public void sendMailSingle(@RequestBody MailDto dto) throws Exception {	
		if(!authService.getCurrentUser().isAdmin()) {
			throw new Exception("");
		}
		adminService.sendMailSingle(dto);
	}
	
	@PostMapping("/mail/all")
	public void sendMailAll(@RequestBody MailDto dto) throws Exception {	
		if(!authService.getCurrentUser().isAdmin()) {
			throw new Exception("");
		}
		adminService.sendMailAll(dto);
	}

}
