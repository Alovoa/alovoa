package com.nonononoki.alovoa.rest;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nonononoki.alovoa.service.PublicService;

@RestController
@RequestMapping("/")
public class PublicController {
	
	@Autowired
	private PublicService publicService;	

	@GetMapping("/text/{value:.+}")
    public String text(Model model, @PathVariable String value) throws MessagingException {
		return publicService.text(value);
    }
}
