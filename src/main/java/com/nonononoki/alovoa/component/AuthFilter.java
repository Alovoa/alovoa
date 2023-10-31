package com.nonononoki.alovoa.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.nonononoki.alovoa.model.AuthToken;

public class AuthFilter extends UsernamePasswordAuthenticationFilter {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String CAPTCHA_ID = "captchaId";
    private static final String CAPTCHA_TEXT = "captchaText";
    public static final String REDIRECT_URL = "redirect-url";

    @Value("${app.captcha.login.enabled}")
    private String captchaLoginEnabled;

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        long captchaId;
        String captchaText;

        String username = request.getParameter(USERNAME);
        String password = request.getParameter(PASSWORD);

        if (Boolean.parseBoolean(captchaLoginEnabled)) {
            captchaId = Long.parseLong(request.getParameter(CAPTCHA_ID));
            captchaText = request.getParameter(CAPTCHA_TEXT);
        } else {
            captchaId = -1;
            captchaText = null;
        }

        request.getSession().setAttribute(REDIRECT_URL, request.getParameter(REDIRECT_URL));

        AuthToken auth = new AuthToken(username, password, captchaId, captchaText);
        AuthenticationManager am = this.getAuthenticationManager();
        return am.authenticate(auth);

    }
}
