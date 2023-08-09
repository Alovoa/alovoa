package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.service.CaptchaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/captcha")
public class CaptchaController {
	private static final Logger logger = LoggerFactory.getLogger(CaptchaController.class);

	@Autowired
	private CaptchaService captchaService;

	@GetMapping("/generate")
	public Captcha generate() throws NoSuchAlgorithmException, IOException {

		return captchaService.generate();
	}

}
