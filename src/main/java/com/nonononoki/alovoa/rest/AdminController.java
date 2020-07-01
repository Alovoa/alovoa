package com.nonononoki.alovoa.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nonononoki.alovoa.service.AdminService;

@RestController
@RequestMapping("/admin")
public class AdminController {

	@Autowired
	private AdminService adminService;

	@GetMapping("/test/notification/like")
	public void testNotificationLike() throws Exception {
		adminService.testNotificationLike();
	}

	@GetMapping("/test/notification/match")
	public void testNotificationMatch() throws Exception {
		adminService.testNotificationMatch();
	}

	@GetMapping("/test/notification/message")
	public void testNotificationMessage() throws Exception {
		adminService.testNotificationMessage();
	}
}
