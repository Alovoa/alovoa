package com.nonononoki.alovoa.model;

public class AlovoaException extends Exception {
	private static final long serialVersionUID = 3421165817331537192L;
	
	public static final String MAX_MEDIA_SIZE_EXCEEDED = "max_media_size_exceeded";
	
	public AlovoaException(String errorMessage) {
        super(errorMessage);
    }
}
