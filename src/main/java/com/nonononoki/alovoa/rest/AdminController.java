package com.nonononoki.alovoa.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nonononoki.alovoa.model.MailDto;
import com.nonononoki.alovoa.service.AdminService;
import com.nonononoki.alovoa.service.AuthService;

@RestController
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private AdminService adminService;
	
	@Autowired
	private AuthService authService;
	

	@PostMapping("/report/delete/{id}")
	public void deleteReport(@PathVariable long id) throws Exception {	
		if(!authService.getCurrentUser().isAdmin()) {
			throw new Exception("");
		}
		adminService.deleteReport(id);
	}
	
	@PostMapping("/ban-user/{id}")
	public void banUser(@PathVariable String id) throws Exception {	
		if(!authService.getCurrentUser().isAdmin()) {
			throw new Exception("");
		}
		adminService.banUser(id);
	}
	
	@PostMapping("/contact/hide/{id}")
	public void hideContact(@PathVariable long id) throws Exception {	
		if(!authService.getCurrentUser().isAdmin()) {
			throw new Exception("");
		}
		adminService.hideContact(id);
	}
	
	@PostMapping(value="/mail/single", consumes = "application/json")
	public void sendMailSingle(@RequestBody MailDto dto) throws Exception {	
		if(!authService.getCurrentUser().isAdmin()) {
			throw new Exception("");
		}
		adminService.sendMailSingle(dto);
	}
	
	@PostMapping(value="/mail/all", consumes = "application/json")
	public void sendMailAll(@RequestBody MailDto dto) throws Exception {	
		if(!authService.getCurrentUser().isAdmin()) {
			throw new Exception("");
		}
		adminService.sendMailAll(dto);
	}

}
