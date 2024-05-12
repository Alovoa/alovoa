package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.ExceptionHandler;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Contact;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDonation;
import com.nonononoki.alovoa.entity.user.UserReport;
import com.nonononoki.alovoa.entity.user.UserVerificationPicture;
import com.nonononoki.alovoa.model.*;
import com.nonononoki.alovoa.repo.*;
import jakarta.mail.MessagingException;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminService.class);
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private MailService mailService;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private ContactRepository contactRepo;
    @Autowired
    private UserLikeRepository userLikeRepo;
    @Autowired
    private UserHideRepository userHideRepo;
    @Autowired
    private UserBlockRepository userBlockRepo;
    @Autowired
    private UserReportRepository userReportRepo;
    @Autowired
    private UserNotificationRepository userNotificationRepo;
    @Autowired
    private ConversationRepository conversationRepo;
    @Autowired
    private UserVerificationPictureRepository userVerificationPictureRepo;
    @Autowired
    private TextEncryptorConverter textEncryptor;

    @Value("${app.search.ignore-intention}")
    private boolean ignoreIntention;

    public void hideContact(long id) throws AlovoaException {
        checkRights();

        Contact contact = contactRepo.findById(id).orElse(null);

        if (contact == null) {
            throw new AlovoaException("contact_not_found");
        }
        contact.setHidden(true);
        contactRepo.saveAndFlush(contact);
    }

    public void sendMailSingle(MailDto dto) throws AlovoaException {
        checkRights();

        mailService.sendAdminMail(dto.getEmail(), dto.getSubject(), dto.getBody());
    }

    public void sendMailAll(MailDto dto) throws AlovoaException, MessagingException, IOException {
        checkRights();

        List<User> users = userRepo.findByDisabledFalseAndAdminFalseAndConfirmedTrue();
        mailService.sendAdminMailAll(dto.getSubject(), dto.getBody(), users);
    }

    public void deleteReport(long id) throws AlovoaException {
        checkRights();

        UserReport r = userReportRepo.findById(id).orElse(null);
        if (r == null) {
            throw new AlovoaException("report_not_found");
        }

        for (UserReport report : userReportRepo.findByUserTo(r.getUserTo())) {
            try {
                User u = report.getUserFrom();
                u.getReported().remove(report);
                userRepo.saveAndFlush(u);
            } catch (Exception e) {
                userReportRepo.delete(report);
            }
        }
    }

    public UserDto viewProfile(UUID uuid) throws AlovoaException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, UnsupportedEncodingException {
        checkRights();
        User user = authService.getCurrentUser(true);
        User u = userService.findUserByUuid(uuid);
        return UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                .currentUser(user).user(u).userService(userService).build());
    }

    public void removeImages(UUID uuid) throws AlovoaException, NumberFormatException {
        checkRights();
        User user = userService.findUserByUuid(uuid);
        if (user == null) {
            throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
        }
        user.setProfilePicture(null);
        user.setVerificationPicture(null);
        user.getImages().clear();
        userRepo.saveAndFlush(user);
    }

    public void removeDescription(UUID uuid) throws AlovoaException, NumberFormatException {
        checkRights();
        User user = userService.findUserByUuid(uuid);
        if (user == null) {
            throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
        }
        user.setDescription(null);
        userRepo.saveAndFlush(user);
    }

    public void banUser(UUID uuid) throws AlovoaException, NumberFormatException {
        checkRights();

        User user = userService.findUserByUuid(uuid);

        if (user == null) {
            throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
        }

        if (user.isAdmin()) {
            throw new AlovoaException("user_is_admin");
        }

        UserDeleteParams userDeleteParam = UserDeleteParams.builder().conversationRepo(conversationRepo)
                .userBlockRepo(userBlockRepo).userHideRepo(userHideRepo).userLikeRepo(userLikeRepo)
                .userNotificationRepo(userNotificationRepo).userRepo(userRepo).userReportRepo(userReportRepo)
                .userVerificationPictureRepo(userVerificationPictureRepo).build();

        try {
            UserService.removeUserDataCascading(user, userDeleteParam);
        } catch (Exception e) {
            logger.warn(ExceptionUtils.getStackTrace(e));
        }

        user = userRepo.findByEmail(user.getEmail());

        user.setAudio(null);
        user.setDates(null);
        user.setDeleteToken(null);
        user.setDescription(null);
        user.setLanguage(null);
        user.setCountry(null);
        user.setDisabled(true);
        user.getDonations().clear();
        user.setFirstName(null);
        user.setGender(null);
        user.getImages().clear();
        user.setIntention(null);
        user.getInterests().clear();
        user.setLocationLatitude(null);
        user.setLocationLongitude(null);
        user.setPassword(null);
        user.setPasswordToken(null);
        user.setPreferedGenders(null);
        user.setPreferedMaxAge(0);
        user.setPreferedMinAge(0);
        user.setRegisterToken(null);
        user.setTotalDonations(0);
        user.setProfilePicture(null);
        user.setVerificationCode(null);
        user.setVerificationPicture(null);
        user.setShowZodiac(false);
        user.getPrompts().clear();
        userRepo.saveAndFlush(user);
    }

    public void deleteAccount(AdminAccountDeleteDto dto) throws AlovoaException {
        checkRights();

        User user = userRepo.findByEmail(Tools.cleanEmail(dto.getEmail()));
        if (user == null) {
            try {
                UUID uuid = UUID.fromString(dto.getEmail());
                user = userService.findUserByUuid(uuid);
            } catch (Exception ignored) {
            }
        }

        if (user == null) {
            throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
        }

        if (user.isDisabled()) {
            throw new AlovoaException("user_is_banned");
        }

        if (user.isAdmin()) {
            throw new AlovoaException("cannot_delete_admin");
        }

        UserDeleteParams userDeleteParam = UserDeleteParams.builder().conversationRepo(conversationRepo)
                .userBlockRepo(userBlockRepo).userHideRepo(userHideRepo).userLikeRepo(userLikeRepo)
                .userNotificationRepo(userNotificationRepo).userRepo(userRepo).userReportRepo(userReportRepo)
                .userVerificationPictureRepo(userVerificationPictureRepo).build();
        UserService.removeUserDataCascading(user, userDeleteParam);
        userRepo.delete(userRepo.findByEmail(user.getEmail()));
        userRepo.flush();
    }

    public boolean userExists(String email) throws AlovoaException {
        checkRights();
        User u = userRepo.findByEmail(Tools.cleanEmail(URLDecoder.decode(email, StandardCharsets.UTF_8)));
        return u != null;
    }

    public void addDonation(String email, double amount) throws AlovoaException {
        checkRights();
        User user = userRepo.findByEmail(Tools.cleanEmail(URLDecoder.decode(email, StandardCharsets.UTF_8)));
        if (user == null) {
            try {
                UUID uuid = UUID.fromString(email);
                user = userService.findUserByUuid(uuid);
            } catch (Exception ignored) {
            }
        }

        if (user != null) {
            UserDonation userDonation = new UserDonation();
            userDonation.setAmount(amount);
            userDonation.setDate(new Date());
            userDonation.setUser(user);
            user.getDonations().add(userDonation);
            user.setTotalDonations(user.getTotalDonations() + amount);
            user.getDates().setLatestDonationDate(new Date());
            userRepo.saveAndFlush(user);
        } else {
            throw new AlovoaException("User not found!");
        }
    }

    public void verifyVerificationPicture(UUID uuid) throws AlovoaException {
        checkRights();
        User user = userService.findUserByUuid(uuid);
        UserVerificationPicture verificationPicture = user.getVerificationPicture();
        if (verificationPicture == null) {
            return;
        }
        verificationPicture.setVerifiedByAdmin(true);
        verificationPicture.setUserNo(null);
        verificationPicture.setUserYes(null);
        user.setVerificationPicture(verificationPicture);
        userRepo.saveAndFlush(user);
    }

    public void deleteVerificationPicture(UUID uuid) throws AlovoaException {
        checkRights();
        User user = userService.findUserByUuid(uuid);
        user.setVerificationPicture(null);
        userRepo.saveAndFlush(user);
    }

    public void checkRights() throws AlovoaException {
        if (!authService.getCurrentUser(true).isAdmin()) {
            throw new AlovoaException("not_admin");
        }
    }

}
