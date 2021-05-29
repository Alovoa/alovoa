package com.nonononoki.alovoa.rest;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.ContactDto;
import com.nonononoki.alovoa.service.ImprintService;

@RestController
@RequestMapping("/imprint")
public class ImprintController {

	@Autowired
	private ImprintService imprintService;

	@PostMapping(value = "/contact", consumes = "application/json")
	public void contact(@RequestBody ContactDto dto)
			throws UnsupportedEncodingException, NoSuchAlgorithmException, AlovoaException {
		imprintService.contact(dto);
	}
}
