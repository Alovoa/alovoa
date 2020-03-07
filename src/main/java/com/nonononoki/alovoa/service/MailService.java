package com.nonononoki.alovoa.service;

import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserPasswordToken;
import com.nonononoki.alovoa.entity.UserRegisterToken;

@Service
public class MailService {

	@Value("${spring.mail.username}")
	private String defaultFrom;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private JavaMailSender mailSender;

	@Value("${app.name}")
	private String appName;
	
	@Value("${app.domain}")
	private String appDomain;

	public void sendMail(String to, String from, String subject, String body) throws MessagingException {
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
		helper.setFrom(from);
		helper.setTo(to);
		helper.setSubject(subject);
		helper.setText(body, true);
		mailSender.send(mimeMessage);
	}

	public void sendRegistrationMail(User user, UserRegisterToken token) throws MessagingException {
		Locale locale = LocaleContextHolder.getLocale();
		String subject = messageSource.getMessage("backend.mail.register.subject", new String[] { appName }, "",
				locale);
		String body = messageSource.getMessage("backend.mail.register.body",
				new String[] { user.getFirstName(), appName, appDomain, token.getContent() }, "", locale);
		sendMail(user.getEmail(), defaultFrom, subject, body);
	}
	
	public void sendPasswordResetMail(User user, UserPasswordToken token) throws MessagingException {
		Locale locale = LocaleContextHolder.getLocale();
		String subject = messageSource.getMessage("backend.mail.password-reset.subject", new String[] { appName }, "",
				locale);
		String body = messageSource.getMessage("backend.mail.password-reset.body",
				new String[] { user.getFirstName(), appName, appDomain, token.getContent() }, "", locale);
		sendMail(user.getEmail(), defaultFrom, subject, body);
	}
}
