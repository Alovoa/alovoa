package com.nonononoki.alovoa.rest;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import jakarta.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.PasswordChangeDto;
import com.nonononoki.alovoa.model.PasswordResetDto;
import com.nonononoki.alovoa.service.PasswordService;

@RestController
@RequestMapping("/password")
public class PasswordController {

	@Autowired
	private PasswordService passwordService;

	@PostMapping(value = "/reset", consumes = "application/json")
	public void resetPassword(@RequestBody PasswordResetDto dto)
			throws NoSuchAlgorithmException, AlovoaException, MessagingException, IOException {
		passwordService.resetPassword(dto);
	}

	@PostMapping(value = "/change", consumes = "application/json")
	public void changePassword(@RequestBody PasswordChangeDto dto) throws AlovoaException {
		passwordService.changePassword(dto);
	}
}
