package com.nonononoki.alovoa.html;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserHide;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class DislikedUsersResource {

    private static final long MAX_RESULTS = 50;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private TextEncryptorConverter textEncryptor;

    @GetMapping("/user/disliked-users")
    public ModelAndView dislikedUsers() throws AlovoaException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            UnsupportedEncodingException {

        User user = authService.getCurrentUser(true);
        ModelAndView mav = new ModelAndView("disliked-users");
        List<UserHide> userDislikes = user.getHiddenUsers();
        List<User> dislikedUsers = userDislikes.stream().sorted(Comparator.comparing(UserHide::getDate).reversed())
                .limit(MAX_RESULTS).map(UserHide::getUserTo).toList();
        List<UserDto> users = new ArrayList<>();
        for (User u : dislikedUsers) {
            users.add(UserDto.userToUserDto(u, user, userService, textEncryptor, UserDto.PROFILE_PICTURE_ONLY));
        }
        mav.addObject("users", users);
        mav.addObject("user", UserDto.userToUserDto(user, user, userService, textEncryptor, UserDto.NO_MEDIA));
        return mav;
    }
}
