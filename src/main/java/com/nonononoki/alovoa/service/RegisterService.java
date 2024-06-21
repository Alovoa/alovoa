package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.ExceptionHandler;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDates;
import com.nonononoki.alovoa.entity.user.UserIntention;
import com.nonononoki.alovoa.entity.user.UserRegisterToken;
import com.nonononoki.alovoa.entity.user.UserSettings;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.BaseRegisterDto;
import com.nonononoki.alovoa.model.RegisterDto;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.repo.UserRegisterTokenRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

@Service
public class RegisterService {

    @Value("${app.token.length}")
    private int tokenLength;

    @Value("${app.age.min}")
    private int minAge;

    @Value("${app.age.max}")
    private int maxAge;

    @Value("${app.age.range}")
    private int ageRange;

    @Value("${spring.profiles.active}")
    private String profile;

    @Value("${app.intention.delay}")
    private long intentionDelay;

    @Value("${app.first-name.length-max}")
    private long firstNameLengthMax;

    @Value("${app.first-name.length-min}")
    private long firstNameLengthMin;

    @Value("${app.mail.plus-addressing}")
    private boolean plusAddressing;

    @Value("${app.referral.max}")
    private int referralMax;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MailService mailService;

    @Autowired
    private PublicService publicService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private GenderRepository genderRepo;

    @Autowired
    private UserIntentionRepository userIntentionRepo;

    @Autowired
    private UserRegisterTokenRepository registerTokenRepo;

    @Autowired
    private AuthService authService;

    @Autowired
    protected CaptchaService captchaService;

    @Autowired
    private UserService userService;

    @Autowired
    private TextEncryptorConverter textEncryptor;

    private static final int MIN_PASSWORD_SIZE = 7;

    private static final Logger logger = LoggerFactory.getLogger(RegisterService.class);

    public String register(RegisterDto dto)
            throws NoSuchAlgorithmException, AlovoaException, MessagingException, IOException {

        dto.setEmail(Tools.cleanEmail(dto.getEmail()));

        if (!isValidEmailAddress(dto.getEmail())) {
            throw new AlovoaException("email_invalid");
        }

        if (!profile.equals(Tools.DEV)) {
            dto.setEmail(Tools.cleanEmail(dto.getEmail()));
            if (plusAddressing && dto.getEmail().contains("+")) {
                dto.setEmail(dto.getEmail().split("[+]")[0] + "@" + dto.getEmail().split("@")[1]);
            }
        }

        // check if email is in spam mail list
        if (profile.equals(Tools.PROD)) {
            try {
                // check spam domains
                if (Tools.isTextContainingLineFromFile(Tools.TEMP_EMAIL_FILE_NAME, dto.getEmail())) {
                    throw new AlovoaException(publicService.text("backend.error.register.email-spam"));
                }
            } catch (IOException e) {
                logger.error(e.getMessage());
            }
        }

        User user = userRepo.findByEmail(dto.getEmail());
        if (user != null) {
            throw new AlovoaException(publicService.text("backend.error.register.email-exists"));
        }

        BaseRegisterDto baseRegisterDto = registerBase(dto, false);
        user = baseRegisterDto.getUser();
        user.setReferrerCode(dto.getReferrerCode());

        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user = userRepo.saveAndFlush(user);

        UserRegisterToken token = createUserToken(user);
        return token.getContent();
    }

    public void registerOauth(RegisterDto dto) throws MessagingException, IOException, AlovoaException,
            NumberFormatException {

        String email = Tools.cleanEmail(authService.getOauth2Email());
        if (email == null) {
            throw new AlovoaException("email_is_null");
        }

        User user = userRepo.findByEmail(email);
        if (user != null) {
            throw new AlovoaException(publicService.text("backend.error.register.email-exists"));
        }

        dto.setEmail(email);
        BaseRegisterDto baseRegisterDto = registerBase(dto, true);
        user = baseRegisterDto.getUser();
        user.setConfirmed(true);

        try {
            if (dto.getReferrerCode() != null && !dto.getReferrerCode().isEmpty()) {
                User referrer;
                referrer = userService.findUserByUuid(UUID.fromString(dto.getReferrerCode()));

                if (referrer != null && referrer.isConfirmed() && referrer.getNumberReferred() < referralMax) {
                    user.setTotalDonations(Tools.REFERRED_AMOUNT);
                    user.setNumberReferred(1);
                    referrer.setTotalDonations(referrer.getTotalDonations() + Tools.REFERRED_AMOUNT);
                    referrer.setNumberReferred(referrer.getNumberReferred() + 1);
                }
            }
        } catch (Exception e) {
            throw new AlovoaException(e.getMessage());
        }

        userRepo.saveAndFlush(user);

        userService.updateUserInfo(user);

        mailService.sendAccountConfirmed(user);
    }

    public UserRegisterToken createUserToken(User user) throws MessagingException, IOException {
        UserRegisterToken token = generateToken(user);
        user.setRegisterToken(token);
        user = userRepo.saveAndFlush(user);
        mailService.sendRegistrationMail(user);
        return token;
    }

    public UserRegisterToken generateToken(User user) {
        UserRegisterToken token = new UserRegisterToken();
        token.setContent(RandomStringUtils.random(tokenLength, 0, 0, true, true, null, new SecureRandom()));
        token.setDate(new Date());
        token.setUser(user);
        return token;
    }

    public User registerConfirm(String tokenString) throws MessagingException, IOException, AlovoaException,
            NumberFormatException {
        UserRegisterToken token = registerTokenRepo.findByContent(tokenString);

        if (token == null) {
            throw new AlovoaException("token_not_found");
        }

        User user = token.getUser();

        if (user == null) {
            throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
        }

        if (user.isConfirmed()) {
            throw new AlovoaException("user_already_confirmed");
        }

        try {
            if (user.getReferrerCode() != null && !user.getReferrerCode().isEmpty()) {
                long id = UserDto.decodeIdThrowing(user.getReferrerCode(), textEncryptor);
                User referrer = userRepo.findById(id).orElse(null);

                if (referrer != null && referrer.isConfirmed() && referrer.getNumberReferred() < referralMax) {
                    user.setTotalDonations(Tools.REFERRED_AMOUNT);
                    user.setNumberReferred(1);
                    referrer.setTotalDonations(referrer.getTotalDonations() + Tools.REFERRED_AMOUNT);
                    referrer.setNumberReferred(referrer.getNumberReferred() + 1);
                }
            }
        } catch (Exception e) {
            throw new AlovoaException(e.getMessage());
        }

        user.setConfirmed(true);
        user.setRegisterToken(null);
        user.setReferrerCode(null);

        user = userRepo.saveAndFlush(user);

        mailService.sendAccountConfirmed(user);

        return user;
    }

    // used by normal registration and oauth
    private BaseRegisterDto registerBase(RegisterDto dto, boolean isOauth) throws AlovoaException {

        if (dto.getFirstName().length() > firstNameLengthMax || dto.getFirstName().length() < firstNameLengthMin) {
            throw new AlovoaException("name_invalid");
        }

        // check minimum age
        int userAge = Tools.calcUserAge(dto.getDateOfBirth());
        if (userAge < minAge) {
            throw new AlovoaException(publicService.text("backend.error.register.min-age"));
        }
        if (userAge > maxAge) {
            throw new AlovoaException("max_age_exceeded");
        }

        if (!isOauth) {
            if (dto.getPassword().length() < MIN_PASSWORD_SIZE) {
                throw new AlovoaException("password_too_short");
            }

            if (!dto.getPassword().matches(".*\\d.*") || !dto.getPassword().matches(".*[a-zA-Z].*")) {
                throw new AlovoaException("password_too_simple");
            }
        }

        User user = new User(Tools.cleanEmail(dto.getEmail()));
        user.setFirstName(dto.getFirstName());

        // default age bracket, user can change it later in their profile
        int userMinAge = userAge - ageRange;
        int userMaxAge = userAge + ageRange;
        if (userMinAge < minAge) {
            userMinAge = minAge;
        }
        if (userMaxAge > maxAge) {
            userMaxAge = maxAge;
        }

        user.setUuid(UUID.randomUUID());
        user.setPreferedMinAge(dto.getDateOfBirth(), userMinAge);
        user.setPreferedMaxAge(dto.getDateOfBirth(), userMaxAge);
        user.setGender(genderRepo.findById(dto.getGender()).orElse(null));
        user.setIntention(userIntentionRepo.findById(UserIntention.MEET).orElse(null));
        user.setPreferedGenders(new HashSet<>(genderRepo.findAll()));

        UserDates dates = new UserDates();
        Date today = new Date();
        dates.setActiveDate(today);
        dates.setCreationDate(today);
        dates.setDateOfBirth(dto.getDateOfBirth());
        dates.setIntentionChangeDate(new Date(today.getTime() - intentionDelay));
        dates.setMessageCheckedDate(today);
        dates.setMessageDate(today);
        dates.setNotificationCheckedDate(today);
        dates.setNotificationDate(today);
        dates.setUser(user);
        user.setDates(dates);

        UserSettings userSettings = new UserSettings();
        user.setUserSettings(userSettings);

        // resolves hibernate issue with null Collections with orphanremoval
        // https://hibernate.atlassian.net/browse/HHH-9940
        user.setInterests(new ArrayList<>());
        user.setImages(new ArrayList<>());
        user.setDonations(new ArrayList<>());
        user.setLikes(new ArrayList<>());
        user.setLikedBy(new ArrayList<>());
        user.setConversations(new ArrayList<>());
        user.setMessageReceived(new ArrayList<>());
        user.setMessageSent(new ArrayList<>());
        user.setNotifications(new ArrayList<>());
        user.setNotificationsFrom(new ArrayList<>());
        user.setHiddenByUsers(new ArrayList<>());
        user.setHiddenUsers(new ArrayList<>());
        user.setBlockedByUsers(new ArrayList<>());
        user.setBlockedUsers(new ArrayList<>());
        user.setReported(new ArrayList<>());
        user.setReportedByUsers(new ArrayList<>());
        user.setPrompts(new ArrayList<>());

        user.setNumberReferred(0);

        user = userRepo.saveAndFlush(user);

        userService.updateUserInfo(user);

        BaseRegisterDto baseRegisterDto = new BaseRegisterDto();
        baseRegisterDto.setRegisterDto(dto);
        baseRegisterDto.setUser(user);
        return baseRegisterDto;
    }

    private static boolean isValidEmailAddress(String email) {
        if (email == null) {
            return false;
        }
        try {
            InternetAddress a = new InternetAddress(email);
            a.validate();
            return true;
        } catch (AddressException ex) {
            return false;
        }
    }
}
