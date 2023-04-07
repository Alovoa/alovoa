package com.nonononoki.alovoa.html;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.ConversationDto;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserBlockRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Controller
public class MessageResource {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ConversationRepository conversationRepo;

    @Autowired
    private UserBlockRepository userBlockRepo;

    @Autowired
    private TextEncryptorConverter textEncryptor;

    @GetMapping("/chats")
    public ModelAndView chats()
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {

        ModelAndView mav = new ModelAndView("messages");
        User user = authService.getCurrentUser(true);
        user.getDates().setMessageCheckedDate(new Date());
        userRepo.saveAndFlush(user);
        List<ConversationDto> convos = new ArrayList<>();
        List<Conversation> conversations = conversationRepo.findByUsers_Id(user.getId());
        for (Conversation c : conversations) {
            if (!c.isBlocked(userBlockRepo)) {
                convos.add(ConversationDto.conversationToDto(c, user, textEncryptor));
            }
        }

        convos.sort((ConversationDto a, ConversationDto b) -> b.getLastUpdated().compareTo(a.getLastUpdated()));

        mav.addObject("conversations", convos);
        mav.addObject("user", UserDto.userToUserDto(user, user, userService, textEncryptor, UserDto.NO_MEDIA));
        return mav;
    }

    @GetMapping("/chats/{id}")
    public ModelAndView chatsDetail(@PathVariable long id) throws AlovoaException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException {

        ModelAndView mav = new ModelAndView("message-detail");
        User user = authService.getCurrentUser(true);
        Conversation c = conversationRepo.findById(id).orElse(null);

        if (c == null) {
            throw new AlovoaException("conversation_not_found");
        }

        if (!c.containsUser(user)) {
            throw new AlovoaException("user_not_in_conversation");
        }

        User u = c.getPartner(user);

        mav.addObject("user", UserDto.userToUserDto(user, user, userService, textEncryptor, UserDto.NO_MEDIA));
        mav.addObject("convoId", id);
        mav.addObject("partner", UserDto.userToUserDto(u, user, userService, textEncryptor, UserDto.PROFILE_PICTURE_ONLY));

        c.setLastOpened(new Date());
        conversationRepo.saveAndFlush(c);
        return mav;
    }
}
