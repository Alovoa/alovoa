package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.component.ExceptionHandler;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AccountDeletionRequestDto;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.ContactDto;
import com.nonononoki.alovoa.repo.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

@Service
public class ImprintService {

	@Autowired
	private CaptchaService captchaService;

	@Autowired
	private PublicService publicService;

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private MailService mailService;

	@Value("${spring.mail.username}")
	private String defaultFrom;

	public void contact(com.nonononoki.alovoa.model.ContactDto dto)
			throws UnsupportedEncodingException, NoSuchAlgorithmException, AlovoaException {
		boolean isValid = captchaService.isValid(dto.getCaptchaId(), dto.getCaptchaText());
		if (!isValid) {
			throw new AlovoaException(publicService.text("backend.error.captcha.invalid"));
		}

		mailService.sendAdminMail(defaultFrom, dto.getEmail(), dto.getMessage());
	}

	public void deleteAccountRequest(AccountDeletionRequestDto dto) throws AlovoaException, MessagingException,
			IOException, NoSuchAlgorithmException {
		boolean isValid = captchaService.isValid(dto.getCaptchaId(), dto.getCaptchaText());
		if (!isValid) {
			throw new AlovoaException(publicService.text("backend.error.captcha.invalid"));
		}
		User user = userRepository.findByEmail(dto.getEmail());

		if (user == null) {
			try{
				UUID uuid = UUID.fromString(dto.getEmail());
				user = userRepository.findByUuid(uuid);
			} catch (IllegalArgumentException ignored){}
		}
		if (user != null) {
			userService.deleteAccountRequestBase(user);
		}
	}
}
