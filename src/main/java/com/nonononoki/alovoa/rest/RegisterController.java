package com.nonononoki.alovoa.rest;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nonononoki.alovoa.model.RegisterDto;
import com.nonononoki.alovoa.service.RegisterService;

@RestController
@RequestMapping("/")
public class RegisterController {
	
	@Autowired
	private RegisterService registerService;
	
	@PostMapping(value="/register", consumes = "application/json")
	public void register(@RequestBody RegisterDto dto) throws MessagingException {	
		registerService.register(dto);
	}
}
