package com.nonononoki.alovoa.service;

import java.util.List;
import java.util.Locale;

import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDeleteToken;
import com.nonononoki.alovoa.entity.user.UserPasswordToken;
import com.nonononoki.alovoa.entity.user.UserRegisterToken;

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
	
	@Value("${app.company.name}")
	private String companyName;
	
	
	public void sendMail(String to, String from, String subject, String body) throws Exception {
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
		helper.setFrom(from);
		helper.setTo(to);
		helper.setSubject(subject);
		helper.setText(getEmailText(body), true);
		mailSender.send(mimeMessage);
	}
	
	public void sendAdminMail(String to, String subject, String body) throws Exception {
		sendMail(to, defaultFrom, subject, body);
	}
	
	public void sendAdminMailAll(String subject, String body, List<User> users) throws Exception {
		body = getEmailText(body);
		for(User u : users) {
			sendMail(u.getEmail(), defaultFrom, subject, body);
		}	
	}

	public void sendMailWithAttachment(String to, String from, String subject, String body, String attachmentName,
			ByteArrayResource attachmentRes) throws Exception {
		MimeMessage mimeMessage = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");
		helper.setFrom(from);
		helper.setTo(to);
		helper.setSubject(subject);
		helper.setText(getEmailText(body), true);
		helper.addAttachment(attachmentName, attachmentRes);
		mailSender.send(mimeMessage);
	}
	
	private String getEmailText(String body) throws Exception {
		String template = Tools.getResourceText("static/templates/email.html");
		String text = new String();
		String hrefWebsite = appDomain + "/"; 
		String hrefDonate = appDomain + "/donate-list"; 
		String imgSrc = Tools.imageToB64("static/img/mail_icon.jpg", "jpeg");
		text = template.replaceAll("MAIL_BODY", body);
		text = text.replaceAll("COMPANY_NAME", companyName);
		text = text.replaceAll("SRC_IMAGE", imgSrc);
		text = text.replaceAll("HREF_WEBSITE", hrefWebsite);
		text = text.replaceAll("HREF_DONATE", hrefDonate);

		return text;
	}

	public void sendRegistrationMail(User user, UserRegisterToken token) throws Exception {
		//Locale locale = Tools.getUserLocale(user);
		Locale locale = LocaleContextHolder.getLocale();
		String subject = messageSource.getMessage("backend.mail.register.subject", new String[] { appName }, "",
				locale);
		String body = messageSource.getMessage("backend.mail.register.body",
				new String[] { user.getFirstName(), appName, appDomain, token.getContent() }, "", locale);
		sendMail(user.getEmail(), defaultFrom, subject, body);
	}

	public void sendPasswordResetMail(User user, UserPasswordToken token) throws Exception {
		//Locale locale = Tools.getUserLocale(user);
		Locale locale = LocaleContextHolder.getLocale();
		String subject = messageSource.getMessage("backend.mail.password-reset.subject", new String[] { appName },
				locale);
		String body = messageSource.getMessage("backend.mail.password-reset.body",
				new String[] { user.getFirstName(), appName, appDomain, token.getContent() }, locale);
		sendMail(user.getEmail(), defaultFrom, subject, body);
	}

	public void sendAccountDeleteRequest(User user, UserDeleteToken token) throws Exception {
		Locale locale = Tools.getUserLocale(user);
		String subject = messageSource.getMessage("backend.mail.account-delete-request.subject",
				new String[] { appName }, locale);
		String body = messageSource.getMessage("backend.mail.account-delete-request.body",
				new String[] { user.getFirstName(), appName, appDomain, token.getContent() }, "", locale);
		sendMail(user.getEmail(), defaultFrom, subject, body);
	}

	public void sendAccountDeleteConfirm(User user) throws Exception {
		Locale locale = Tools.getUserLocale(user);
		String subject = messageSource.getMessage("backend.mail.account-delete-confirm.subject",
				new String[] { appName }, locale);
		String body = messageSource.getMessage("backend.mail.account-delete-confirm.body",
				new String[] { user.getFirstName(), appName }, locale);
		sendMail(user.getEmail(), defaultFrom, subject, body);
	}
	
	public void sendAccountConfirmed(User user) throws Exception {
		//Locale locale = Tools.getUserLocale(user);
		Locale locale = LocaleContextHolder.getLocale();
		String subject = messageSource.getMessage("backend.mail.account-confirmed.subject",
				new String[] { appName }, locale);
		String body = messageSource.getMessage("backend.mail.account-confirmed.body",
				new String[] { user.getFirstName(), appName, appDomain}, "", locale);
		sendMail(user.getEmail(), defaultFrom, subject, body);
	}
}
