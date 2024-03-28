package com.nonononoki.alovoa.html;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.ExceptionHandler;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.*;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.ConversationDto;
import com.nonononoki.alovoa.model.NotificationDto;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.*;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@RestController
@RequestMapping("/api/v1/resource")
public class ApiResource {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ProfileResource profileResource;

    @Autowired
    private AdminSearchResource searchResource;

    @Autowired
    private GenderRepository genderRepo;

    @Autowired
    private UserIntentionRepository userIntentionRepo;

    @Autowired
    private ConversationRepository conversationRepo;

    @Autowired
    private UserBlockRepository userBlockRepo;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private TextEncryptorConverter textEncryptor;

    private static final long MAX_RESULTS = 50;

    @Value("${app.media.max-size}")
    private int mediaMaxSize;

    @Value("${app.interest.max}")
    private int interestMaxSize;

    @Value("${app.search.ignore-intention}")
    private boolean ignoreIntention;

    @GetMapping("/donate")
    public Map<String, Object> resourceDonate() throws InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        ModelMap map = new ModelMap();
        User user = authService.getCurrentUser(true);
        map.addAttribute("user", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(user).user(user).userService(userService).build()));
        return map;
    }

    @GetMapping("/chats")
    public Map<String, Object> resourceChats()
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        ModelMap map = new ModelMap();
        User user = authService.getCurrentUser(true);
        user.getDates().setMessageCheckedDate(new Date());
        userRepo.saveAndFlush(user);
        List<ConversationDto> convos = new ArrayList<>();
        List<Conversation> conversations = conversationRepo.findByUsers_Id(user.getId());
        for (Conversation c : conversations) {
            if (!c.isBlocked(userBlockRepo)) {
                convos.add(ConversationDto.conversationToDto(c, user, userService));
            }
        }

        convos.sort((ConversationDto a, ConversationDto b) -> b.getLastUpdated().compareTo(a.getLastUpdated()));

        map.addAttribute("conversations", convos);
        map.addAttribute("user", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(user).user(user).userService(userService).build()));
        return map;
    }

    @GetMapping("/chats/{id}")
    public Map<String, Object> resourceChatsDetail(@PathVariable long id) throws JsonProcessingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        ModelMap map = new ModelMap();
        User user = authService.getCurrentUser(true);
        Conversation c = conversationRepo.findById(id).orElse(null);

        if (c == null) {
            throw new AlovoaException("conversation_not_found");
        }

        if (!c.containsUser(user)) {
            throw new AlovoaException("user_not_in_conversation");
        }

        User u = c.getPartner(user);

        map.addAttribute("user", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(user).user(user).userService(userService).build()));
        map.addAttribute("convoId", id);
        map.addAttribute("partner", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(user).user(u).userService(userService).build()));

        c.setLastOpened(new Date());
        conversationRepo.saveAndFlush(c);
        return map;
    }

    @GetMapping("/alerts")
    public Map<String, Object> resourceNotification() throws InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        ModelMap map = new ModelMap();
        User user = authService.getCurrentUser(true);
        user.getDates().setNotificationCheckedDate(new Date());
        userRepo.saveAndFlush(user);
        List<UserNotification> nots = user.getNotifications();
        List<NotificationDto> notifications = new ArrayList<>();
        for (UserNotification n : nots) {
            boolean blockedMe = user.getBlockedByUsers().stream()
                    .anyMatch(b -> b.getUserFrom().getId().equals(n.getUserFrom().getId()));
            boolean blockedYou = user.getBlockedUsers().stream()
                    .anyMatch(b -> b.getUserTo().getId().equals(n.getUserFrom().getId()));
            boolean matched = user.getLikes().stream().anyMatch(l -> n.getUserFrom().getId().equals(l.getUserTo().getId()));

            if (!blockedMe && !blockedYou && !matched) {
                NotificationDto dto = NotificationDto.notificationToNotificationDto(n, user, userService, textEncryptor, ignoreIntention);
                notifications.add(dto);
            }
        }

        notifications.sort((NotificationDto a, NotificationDto b) -> b.getDate().compareTo(a.getDate()));

        map.addAttribute("notifications", notifications);
        map.addAttribute("user", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(user).user(user).userService(userService).build()));
        return map;
    }

    @GetMapping("/profile")
    public Map<String, Object> resourceProfile() throws InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        return profileResource.profile().getModel();
    }

    @GetMapping("/profile/view/{uuid}")
    public Map<String, Object> resourceProfileView(@PathVariable UUID uuid) throws
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {

        User userView = userService.findUserByUuid(uuid);
        User user = authService.getCurrentUser(true);

        if (user.getId().equals(userView.getId())) {
            throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
        }

        if (userView.getBlockedUsers().stream().filter(o -> o.getUserTo() != null).anyMatch(o -> o.getUserTo().getId().equals(user.getId()))) {
            throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
        }

        if (userView.isDisabled()) {
            throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
        }

        ModelMap map = new ModelMap();

        userView = userRepo.saveAndFlush(userView);

        UserDto userDto = UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(user).user(userView).userService(userService).build());
        UserDto currUserDto = UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(user).user(user).userService(userService).build());

        map.addAttribute("user", userDto);
        map.addAttribute("currUser", currUserDto);
        map.addAttribute("compatible", Tools.usersCompatible(user, userView, ignoreIntention));

        return map;
    }

    @GetMapping("/search")
    public Map<String, Object> resourceSearch() throws InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        return searchResource.search().getModel();
    }

    @GetMapping("/user/onboarding")
    public Map<String, Object> userOnboarding() throws AlovoaException {
        User user = authService.getCurrentUser(true);
        int age = Tools.calcUserAge(user);
        boolean isLegal = age >= Tools.AGE_LEGAL;
        ModelMap mav = new ModelMap();
        mav.addAttribute("genders", genderRepo.findAll());
        mav.addAttribute("intentions", userIntentionRepo.findAll());
        mav.addAttribute("isLegal", isLegal);
        mav.addAttribute("mediaMaxSize", mediaMaxSize);
        mav.addAttribute("interestMaxSize", interestMaxSize);
        return mav;
    }

    @GetMapping("/blocked-users")
    public Map<String, Object> blockedUsers() throws JsonProcessingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        User user = authService.getCurrentUser(true);
        ModelMap map = new ModelMap();
        List<UserBlock> userBlocks = user.getBlockedUsers();
        List<User> blockedUsers = userBlocks.stream().sorted(Comparator.comparing(UserBlock::getDate).reversed())
                .limit(MAX_RESULTS).map(UserBlock::getUserTo).toList();
        List<UserDto> users = new ArrayList<>();
        for (User u : blockedUsers) {
            users.add(UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                    .currentUser(user).user(u).userService(userService).build()));
        }
        map.addAttribute("users", users);
        map.addAttribute("user", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(user).user(user).userService(userService).build()));
        return map;
    }

    @GetMapping("/liked-users")
    public Map<String, Object> likedUsers() throws InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        User user = authService.getCurrentUser(true);
        ModelMap map = new ModelMap();
        List<UserLike> userLikes = user.getLikes();
        List<User> likedUsers = userLikes.stream().sorted(Comparator.comparing(UserLike::getDate).reversed())
                .limit(MAX_RESULTS).map(UserLike::getUserTo).toList();
        List<UserDto> users = new ArrayList<>();
        for (User u : likedUsers) {
            users.add(UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                    .currentUser(user).user(u).userService(userService).build()));
        }
        map.addAttribute("users", users);
        map.addAttribute("user", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(user).user(user).userService(userService).build()));
        return map;
    }

    @GetMapping("/disliked-users")
    public Map<String, Object> dislikedUsers() throws InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        User user = authService.getCurrentUser(true);
        ModelMap map = new ModelMap();
        List<UserHide> userDislikes = user.getHiddenUsers();
        List<User> dislikedUsers = userDislikes.stream().sorted(Comparator.comparing(UserHide::getDate).reversed())
                .limit(MAX_RESULTS).map(UserHide::getUserTo).toList();
        List<UserDto> users = new ArrayList<>();
        for (User u : dislikedUsers) {
            users.add(UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                    .currentUser(user).user(u).userService(userService).build()));
        }
        map.addAttribute("users", users);
        map.addAttribute("user", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(user).user(user).userService(userService).build()));
        return map;
    }
}
