package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.RegisterService;

@Controller
public class RegisterResource {

    private boolean facebookEnabled;
    private boolean googleEnabled;
    private boolean keycloakEnabled;

    @Autowired
    private RegisterService registerService;

    @Autowired
    private GenderRepository genderRepo;

    @Autowired
    private UserIntentionRepository userIntentionRepo;

    @Autowired
    private AuthService authService;

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


    public static final String URL = "/register";

    @GetMapping(URL)
    public ModelAndView register() throws AlovoaException {

        User user = authService.getCurrentUser();
        if (user != null) {
            return new ModelAndView("redirect:" + ProfileResource.URL);
        }
        ModelAndView mav = new ModelAndView("register");
        mav.addObject("genders", genderRepo.findAll());
        mav.addObject("intentions", userIntentionRepo.findAll());
        mav.addObject("facebookEnabled", facebookEnabled);
        mav.addObject("googleEnabled", googleEnabled);
        mav.addObject("keycloakEnabled", keycloakEnabled);
        mav.addObject("localLoginEnabled", Boolean.parseBoolean(localLoginEnabled));
        return mav;
    }

    public ModelAndView registerOauth(String firstName) {
        ModelAndView mav = new ModelAndView("register-oauth");
        mav.addObject("genders", genderRepo.findAll());
        mav.addObject("intentions", userIntentionRepo.findAll());
        mav.addObject("firstName", firstName);
        return mav;
    }

    @GetMapping("/register/confirm/{tokenString}")
    public String registerConfirm(@PathVariable String tokenString) {
        try {
            registerService.registerConfirm(tokenString);
            return "redirect:/?registration-confirm-success";
        } catch (Exception e) {
            return "redirect:/?registration-confirm-failed";
        }

    }
}
