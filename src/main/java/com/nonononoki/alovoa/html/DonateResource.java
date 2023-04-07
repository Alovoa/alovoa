package com.nonononoki.alovoa.html;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.UserService;
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
public class DonateResource {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private TextEncryptorConverter textEncryptor;

    @GetMapping("/donate")
    public ModelAndView donate()
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        ModelAndView mav = new ModelAndView("donate");
        User user = authService.getCurrentUser(true);
        mav.addObject("user", UserDto.userToUserDto(user, user, userService, textEncryptor, UserDto.NO_MEDIA));
        return mav;
    }
}
