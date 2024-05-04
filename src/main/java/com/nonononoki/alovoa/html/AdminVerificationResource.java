package com.nonononoki.alovoa.html;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserVerificationPicture;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.model.UserVerificationDto;
import com.nonononoki.alovoa.repo.UserVerificationPictureRepository;
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
import java.util.ArrayList;
import java.util.List;

@Controller
public class AdminVerificationResource {

    public static final String URL = "/admin-verification";
    @Autowired
    private UserVerificationPictureRepository userVerificationPictureRepo;
    @Autowired
    private TextEncryptorConverter textEncryptor;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;

    @GetMapping(URL)
    public ModelAndView admin() throws AlovoaException, InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException,
            UnsupportedEncodingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {

        ModelAndView mav = new ModelAndView("admin-verification");

        List<UserVerificationPicture> verificationPictures =
                userVerificationPictureRepo.findTop20ByVerifiedByAdminFalseOrderByDateAsc();

        List<UserVerificationDto> verificationDtos = new ArrayList<>();
        for (UserVerificationPicture v : verificationPictures) {
            if (v.getUser() != null) {
                verificationDtos.add(UserVerificationDto.map(v.getUser(), userService));
            }
        }

        mav.addObject("verifications", verificationDtos);
        User user = authService.getCurrentUser(true);
        mav.addObject("user", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(true)
                .currentUser(user).user(user).userService(userService).build()));

        return mav;
    }
}
