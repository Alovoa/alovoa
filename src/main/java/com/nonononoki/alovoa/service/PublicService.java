package com.nonononoki.alovoa.service;

import java.util.Locale;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PublicService {

	@Autowired
	private MessageSource messageSource;
	
	public String text(String value) throws MessagingException {
		Locale locale = LocaleContextHolder.getLocale();
		String text = messageSource.getMessage(value,
				null, locale);
		return text;
	}
}
