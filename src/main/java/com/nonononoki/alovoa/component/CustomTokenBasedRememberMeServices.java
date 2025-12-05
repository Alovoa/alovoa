package com.nonononoki.alovoa.component;

import com.nonononoki.alovoa.config.SecurityConfig;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.rememberme.TokenBasedRememberMeServices;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Date;

public class CustomTokenBasedRememberMeServices extends TokenBasedRememberMeServices {

    private final UserRepository userRepo;

    public CustomTokenBasedRememberMeServices(String key, UserDetailsService userDetailsService, UserRepository userRepo) {
        super(key, userDetailsService);
        this.userRepo = userRepo;
    }

    @Override
    protected String retrieveUserName(Authentication authentication) {
        if (isInstanceOfUserDetails(authentication)) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return (String) ((OAuth2User) authentication.getPrincipal()).getAttributes().get("email");
    }

    // Mostly copied from TokenBasedRememberMeServices
    @Override
    public void onLoginSuccess(HttpServletRequest request, HttpServletResponse response,
                               Authentication successfulAuthentication) {
        String username = retrieveUserName(successfulAuthentication);
        String password = retrievePassword(successfulAuthentication);
        // If unable to find a username and password, just abort as
        // TokenBasedRememberMeServices is
        // unable to construct a valid token in this case.
        if (!StringUtils.hasLength(username)) {
            this.logger.debug("Unable to retrieve username");
            return;
        }
        if (!StringUtils.hasLength(password)) {
            try {
                UserDetails user = getUserDetailsService().loadUserByUsername(username);
                password = user.getPassword();
            } catch (UsernameNotFoundException e) {
                password = null;
            }
        }
        long expiryTime = System.currentTimeMillis();
        User user = userRepo.findByEmail(username);
        if(user == null) {
            return;
        }
        String uuidString = user.getUuid().toString();
        // SEC-949
        expiryTime += 1000L * TWO_WEEKS_S;
        String signatureValue = makeTokenSignature(expiryTime, uuidString, password);
        setCookie(new String[]{uuidString, Long.toString(expiryTime), signatureValue}, TWO_WEEKS_S, request,
                response);
        this.logger.debug(
                "Added remember-me cookie for user '" + username + "', expiry: '" + new Date(expiryTime) + "'");
    }

    // Mostly copied from TokenBasedRememberMeServices
    public Cookie getRememberMeCookie(String cookieValue, HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie(SecurityConfig.COOKIE_REMEMBER, cookieValue);
        cookie.setMaxAge(TWO_WEEKS_S);
        cookie.setPath(getCookiePath(request));
        cookie.setSecure(request.isSecure());
        cookie.setHttpOnly(true);
        return cookie;
    }

    // Mostly copied from TokenBasedRememberMeServices
    private String getCookiePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return (!contextPath.isEmpty()) ? contextPath : "/";
    }

    // private override
    private boolean isInstanceOfUserDetails(Authentication authentication) {
        return authentication.getPrincipal() instanceof UserDetails;
    }

    // Mostly copied from TokenBasedRememberMeServices
    public String getRememberMeCookieData(String username, String password) {
        long expiryTime = System.currentTimeMillis();
        // SEC-949
        expiryTime += 1000L * TWO_WEEKS_S;
        String signatureValue = makeTokenSignature(expiryTime, username, password);
        return encodeCookie(new String[]{username, Long.toString(expiryTime), signatureValue});
    }

    public String getRememberMeCookieData(String username) {
        return getRememberMeCookieData(username, null);
    }
}
