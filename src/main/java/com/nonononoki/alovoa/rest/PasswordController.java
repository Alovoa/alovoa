package com.nonononoki.alovoa.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nonononoki.alovoa.model.PasswordChangeDto;
import com.nonononoki.alovoa.model.PasswordResetDto;
import com.nonononoki.alovoa.service.PasswordService;

@RestController
@RequestMapping("/password")
public class PasswordController {
	
	@Autowired
	private PasswordService passwordService;
	
	@PostMapping(value="/reset", consumes = "application/json")
	public void resetPasword(@RequestBody PasswordResetDto dto) throws Exception {
		passwordService.resetPasword(dto);
	}
	
	@PostMapping(value="/change", consumes = "application/json")
	public void changePasword(@RequestBody PasswordChangeDto dto) throws Exception {
		passwordService.changePasword(dto);
	}
}
