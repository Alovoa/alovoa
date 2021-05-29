package com.nonononoki.alovoa.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class PublicService {

	@Autowired
	private MessageSource messageSource;
	
	public String text(String value) {
		Locale locale = LocaleContextHolder.getLocale();
		return messageSource.getMessage(value,
				null, locale);
	}
}
