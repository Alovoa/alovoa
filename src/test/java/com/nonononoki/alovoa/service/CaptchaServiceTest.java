package com.nonononoki.alovoa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.nonononoki.alovoa.repo.CaptchaRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CaptchaServiceTest {


	@Autowired
	private CaptchaService captchaService;
	
	@Autowired
	private CaptchaRepository captchaRepo;

	@Test
	void test() throws Exception {
		captchaService.generate();
		captchaService.generate();
		assertEquals(1, captchaRepo.count());
	}
}
