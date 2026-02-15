package com.nonononoki.alovoa.component;

import lombok.AllArgsConstructor;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.StaleObjectStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.HtmlUtils;

import com.nonononoki.alovoa.model.AlovoaException;

@ControllerAdvice
@AllArgsConstructor
public class ExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);

	public static final String USER_NOT_FOUND = "user_not_found";
	public static final String USER_NOT_COMPATIBLE = "users_not_compatible";

	private Environment env;

	@org.springframework.web.bind.annotation.ExceptionHandler
	protected ResponseEntity<Object> handleConflict(Exception ex, WebRequest request) {
		String exceptionMessage = ex.getMessage();
		if (ex instanceof AlovoaException && !env.acceptsProfiles(Profiles.of("dev"))) {
			LOGGER.error(ExceptionUtils.getMessage(ex));
		} else if (!(ex instanceof ObjectOptimisticLockingFailureException)
                && !(ex instanceof StaleObjectStateException)
                && !(ex instanceof ClientAbortException)) {
            LOGGER.error(ExceptionUtils.getStackTrace(ex));
        }
		exceptionMessage = exceptionMessage == null ? null : HtmlUtils.htmlEscape(exceptionMessage);
		return handleExceptionInternal(ex, exceptionMessage, new HttpHeaders(), HttpStatus.CONFLICT, request);
	}
}