package com.nonononoki.alovoa.html;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.ExceptionHandler;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

@Controller
public class UserProfileResource {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private TextEncryptorConverter textEncryptor;

    @GetMapping("/profile/view/{idEncoded}")
    public ModelAndView profileView(@PathVariable String idEncoded) throws NumberFormatException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        return data(idEncoded, "user-profile");
    }

    @GetMapping("/profile/view/modal/{idEncoded}")
    public ModelAndView profileViewModal(@PathVariable String idEncoded) throws AlovoaException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
        return data(idEncoded, "fragments::user-profile-modal");
    }

    public ModelAndView data(String idEncoded, String view) throws AlovoaException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException {
        Optional<Long> idOptional = UserDto.decodeId(idEncoded, textEncryptor);
        if (idOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Long id = idOptional.get();
        User userView = userRepo.findById(id).orElse(null);
        User user = authService.getCurrentUser(true);

        if (user.getId().equals(id)) {
            return new ModelAndView("redirect:" + ProfileResource.URL);
        }

        if (userView != null) {
            if (userView.getBlockedUsers().stream().filter(o -> o.getUserTo() != null).anyMatch(o -> o.getUserTo().getId().equals(user.getId()))) {
                throw new AlovoaException("blocked");
            }

            if (userView.isDisabled()) {
                throw new AlovoaException("disabled");
            }

            ModelAndView mav = new ModelAndView(view);

            userView.setNumberProfileViews(userView.getNumberProfileViews() + 1);
            userView = userRepo.saveAndFlush(userView);

            UserDto userDto = UserDto.userToUserDto(userView, user, userService, textEncryptor, UserDto.ALL);
            UserDto currUserDto = UserDto.userToUserDto(user, user, userService, textEncryptor, UserDto.NO_MEDIA);

            mav.addObject("user", userDto);
            mav.addObject("currUser", currUserDto);

            mav.addObject("compatible", Tools.usersCompatible(user, userView));

            return mav;

        } else {
            throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
        }
    }
}
