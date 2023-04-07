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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Controller
@RequestMapping("/password")
public class PasswordResource {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private TextEncryptorConverter textEncryptor;

    @GetMapping("/reset")
    public ModelAndView passwordReset() throws AlovoaException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            UnsupportedEncodingException {
        ModelAndView mav = new ModelAndView("password-reset");
        User user = authService.getCurrentUser();
        mav.addObject("user", UserDto.userToUserDto(user, user, userService, textEncryptor, UserDto.NO_MEDIA));
        return mav;
    }

    @GetMapping("/change/{tokenString}")
    public ModelAndView passwordChange(@PathVariable String tokenString) throws AlovoaException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException {
        ModelAndView mav = new ModelAndView("password-change");
        mav.addObject("tokenString", tokenString);
        User user = authService.getCurrentUser();
        mav.addObject("user", UserDto.userToUserDto(user, user, userService, textEncryptor, UserDto.NO_MEDIA));
        return mav;
    }
}
