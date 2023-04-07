package com.nonononoki.alovoa.html;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserVerificationPicture;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.UserVerificationPictureRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
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
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AdminVerificationResource {

    public static final String URL = "/admin/verification";
    @Autowired
    private UserVerificationPictureRepository userVerificationPictureRepo;
    @Autowired
    private TextEncryptorConverter textEncryptor;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;

    @GetMapping(URL)
    public ModelAndView admin() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            UnsupportedEncodingException, AlovoaException {

        ModelAndView mav = new ModelAndView("admin-verification");

        List<UserVerificationDto> verificationPictures =
                userVerificationPictureRepo.findTop20ByOrderByDateAsc().stream().map(e -> new UserVerificationDto(e,
                        e.getUser().getId())).collect(Collectors.toList());

        mav.addObject("verifications", verificationPictures);
        User user = authService.getCurrentUser(true);
        mav.addObject("user", UserDto.userToUserDto(user, user, userService, textEncryptor, UserDto.NO_MEDIA));

        return mav;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    private static class UserVerificationDto {
        UserVerificationPicture verificationPicture;
        Long userId;
    }
}
