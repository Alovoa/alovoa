package com.nonononoki.alovoa.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;

public class AlovoaException extends Exception {
	private static final Logger logger = LoggerFactory.getLogger(AlovoaException.class);
	@Serial
	private static final long serialVersionUID = 3421165817331537192L;
	
	public static final String MAX_MEDIA_SIZE_EXCEEDED = "max_media_size_exceeded";
	
	public AlovoaException(String errorMessage) {
		super(errorMessage);
		logger.error(String.format("AlovoaException: %s: %s", this.getClass().getName(), errorMessage));
    }
}
