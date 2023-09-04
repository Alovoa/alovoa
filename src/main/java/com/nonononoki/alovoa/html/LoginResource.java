package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class LoginResource {

    private boolean facebookEnabled;
    private boolean googleEnabled;
    private boolean keycloakEnabled;

    @Autowired
    private AuthService authService;

    @Value("${app.privacy.update-date}")
    private String privacyDate;

    @Value("${spring.security.oauth2.client.registration.facebook.client-id:#{null}}")
    private void facebookEnabled(String clientId) {
        this.facebookEnabled = clientId != null;
    }

    @Value("${spring.security.oauth2.client.registration.google.client-id:#{null}}")
    private void googleEnabled(String clientId) {
        this.googleEnabled = clientId != null;
    }

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id:#{null}}")
    private void ip6liEnabled(String clientId) {
        this.keycloakEnabled = clientId != null;
    }

    @Value("${app.local.login.enabled}")
    private String localLoginEnabled;

    public static final String URL = "/login";

    @GetMapping(URL)
    public ModelAndView login() throws AlovoaException {

        User user = authService.getCurrentUser();
        if (user != null) {
            return new ModelAndView("redirect:" + SearchResource.URL);
        }

        ModelAndView mav = new ModelAndView("login");
        mav.addObject("facebookEnabled", facebookEnabled);
        mav.addObject("googleEnabled", googleEnabled);
        mav.addObject("keycloakEnabled", keycloakEnabled);
        mav.addObject("localLoginEnabled", Boolean.parseBoolean(localLoginEnabled));
        return mav;
    }
}
