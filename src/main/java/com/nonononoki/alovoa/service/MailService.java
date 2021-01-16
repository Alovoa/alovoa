package com.nonononoki.alovoa.service;

import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserDeleteToken;
import com.nonononoki.alovoa.entity.UserPasswordToken;
import com.nonononoki.alovoa.entity.UserRegisterToken;
import com.nonononoki.alovoa.model.UserGdpr;

@Service
public class MailService {

	@Value("${spring.mail.username}")
	private String defaultFrom;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private JavaMailSender mailSender;

	//@Autowired
	//private ObjectMapper objectMapper;

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
	
	public void sendAdminMail(String to, String subject, String body) throws MessagingException {
		sendMail(to, defaultFrom, subject, body);
	}

	public void sendMailWithAttachment(String to, String from, String subject, String body, String attachmentName,
			ByteArrayResource attachmentRes) throws MessagingException {
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
		helper.setFrom(from);
		helper.setTo(to);
		helper.setSubject(subject);
		helper.setText(body, true);
		helper.addAttachment(attachmentName, attachmentRes);
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
		String subject = messageSource.getMessage("backend.mail.password-reset.subject", new String[] { appName },
				locale);
		String body = messageSource.getMessage("backend.mail.password-reset.body",
				new String[] { user.getFirstName(), appName, appDomain, token.getContent() }, locale);
		sendMail(user.getEmail(), defaultFrom, subject, body);
	}

	/*
	public ResponseEntity<Resource> sendUserDataMail(User user) throws Exception {
		//Locale locale = LocaleContextHolder.getLocale();
		//String subject = messageSource.getMessage("backend.mail.userdata.subject", new String[] { appName }, locale);
		//String body = messageSource.getMessage("backend.mail.userdata.body", new String[] { user.getFirstName(), appName }, locale);

		UserGdpr ug = UserGdpr.userToUserGdpr(user);
		String json = objectMapper.writeValueAsString(ug);
		ByteArrayResource resource = new ByteArrayResource(json.getBytes());
		//sendMailWithAttachment(user.getEmail(), defaultFrom, subject, body, "userdata.json", resource);
		
		MediaType mediaType = MediaTypeFactory
                .getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
		HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        
        return new ResponseEntity<Resource>(
        		resource, headers, HttpStatus.OK
            );
	}
	*/

	public void sendAccountDeleteRequest(User user, UserDeleteToken token) throws MessagingException {
		Locale locale = LocaleContextHolder.getLocale();
		String subject = messageSource.getMessage("backend.mail.account-delete-request.subject",
				new String[] { appName }, locale);
		String body = messageSource.getMessage("backend.mail.account-delete-request.body",
				new String[] { user.getFirstName(), appName, appDomain, token.getContent() }, "", locale);
		sendMail(user.getEmail(), defaultFrom, subject, body);
	}

	public void sendAccountDeleteConfirm(User user) throws MessagingException {
		Locale locale = LocaleContextHolder.getLocale();
		String subject = messageSource.getMessage("backend.mail.account-delete-confirm.subject",
				new String[] { appName }, locale);
		String body = messageSource.getMessage("backend.mail.account-delete-confirm.body",
				new String[] { user.getFirstName(), appName }, locale);
		sendMail(user.getEmail(), defaultFrom, subject, body);
	}
}
