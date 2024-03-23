package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.Contact;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AccountDeletionRequestDto;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.ContactDto;
import com.nonononoki.alovoa.repo.ContactRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

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
	private ContactRepository contactRepo;

	public Contact contact(ContactDto dto)
			throws UnsupportedEncodingException, NoSuchAlgorithmException, AlovoaException {
		boolean isValid = captchaService.isValid(dto.getCaptchaId(), dto.getCaptchaText());
		if (!isValid) {
			throw new AlovoaException(publicService.text("backend.error.captcha.invalid"));
		}
		Contact c = new Contact();
		c.setDate(new Date());
		c.setEmail(dto.getEmail());
		c.setMessage(dto.getMessage());
		return contactRepo.saveAndFlush(c);
	}

	public void deleteAccountRequest(AccountDeletionRequestDto dto) throws AlovoaException, MessagingException,
			IOException, NoSuchAlgorithmException {
		boolean isValid = captchaService.isValid(dto.getCaptchaId(), dto.getCaptchaText());
		if (!isValid) {
			throw new AlovoaException(publicService.text("backend.error.captcha.invalid"));
		}
		User user = userRepository.findByEmail(dto.getEmail());
		if (user != null) {
			userService.deleteAccountRequestBase(user);
		}
	}
}
