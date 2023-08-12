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
import java.util.Optional;

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

        UserReport report = userReportRepo.findById(id).orElse(null);
        if (report == null) {
            throw new AlovoaException("report_not_found");
        }
        try {
            User u = report.getUserFrom();
            u.getReported().remove(report);
            userRepo.saveAndFlush(u);
        } catch (Exception e) {
            userReportRepo.delete(report);
        }
    }

    public void removeImages(String id) throws AlovoaException, NumberFormatException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        checkRights();

        User user = userRepo.findById(UserDto.decodeIdThrowing(id, textEncryptor)).orElse(null);

        if (user == null) {
            throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
        }

        user.setProfilePicture(null);
        user.setVerificationPicture(null);
        user.getImages().clear();
        userRepo.saveAndFlush(user);
    }

    public void removeDescription(String id) throws AlovoaException, NumberFormatException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        checkRights();

        User user = userRepo.findById(UserDto.decodeIdThrowing(id, textEncryptor)).orElse(null);

        if (user == null) {
            throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
        }

        user.setDescription(null);
        userRepo.saveAndFlush(user);
    }

    public void banUser(String id) throws AlovoaException, NumberFormatException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        checkRights();

        User user = userRepo.findById(UserDto.decodeIdThrowing(id, textEncryptor)).orElse(null);

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
        user.setAccentColor(null);
        user.setCountry(null);
        user.setUiDesign(null);
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
        user.setNumberProfileViews(0);
        user.setNumberSearches(0);
        user.setProfilePicture(null);
        user.setVerificationCode(null);
        user.setVerificationPicture(null);
        user.getWebPush().clear();
        user.setShowZodiac(false);
        userRepo.saveAndFlush(user);
    }

    public void deleteAccount(AdminAccountDeleteDto dto) throws AlovoaException {
        checkRights();

        User user = userRepo.findByEmail(Tools.cleanEmail(dto.getEmail()));
        if(user == null) {
            try {
                Optional<Long> idOpt = UserDto.decodeId(dto.getEmail(), textEncryptor);
                if(idOpt.isPresent()) {
                    user = userRepo.findById(idOpt.get()).orElse(null);
                }
            } catch (Exception ignored) {}
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
        if(user == null) {
            try {
                Optional<Long> idOpt = UserDto.decodeId(email, textEncryptor);
                if(idOpt.isPresent()) {
                    user = userRepo.findById(idOpt.get()).orElse(null);
                }
            } catch (Exception ignored) {}
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

    public void verifyVerificationPicture(String idEnc) throws AlovoaException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        checkRights();
        User user = userService.encodedIdToUser(idEnc);
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

    public void deleteVerificationPicture(String idEnc) throws AlovoaException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        checkRights();
        User user = userService.encodedIdToUser(idEnc);
        user.setVerificationPicture(null);
        userRepo.saveAndFlush(user);
    }

    private void checkRights() throws AlovoaException {
        if (!authService.getCurrentUser(true).isAdmin()) {
            throw new AlovoaException("not_admin");
        }
    }

}
