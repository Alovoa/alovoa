package com.nonononoki.alovoa.model;

public class AlovoaException extends Exception {
	private static final long serialVersionUID = 3421165817331537192L;
	
	public AlovoaException(String errorMessage) {
        super(errorMessage);
    }
}
