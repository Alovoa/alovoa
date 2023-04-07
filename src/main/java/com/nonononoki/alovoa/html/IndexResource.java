package com.nonononoki.alovoa.html;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Controller
public class IndexResource {

    @Autowired
    private AuthService authService;

    @GetMapping("/")
    public ModelAndView index() throws AlovoaException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            UnsupportedEncodingException {

        User user = authService.getCurrentUser();
        if (user != null) {
            if (user.getProfilePicture() != null
                    && user.getDescription() != null) {
                return new ModelAndView("redirect:" + SearchResource.URL);
            } else {
                return new ModelAndView("redirect:" + ProfileOnboardingResource.URL);
            }
        }

        ModelAndView mav = new ModelAndView("index");
        return mav;
    }
}
