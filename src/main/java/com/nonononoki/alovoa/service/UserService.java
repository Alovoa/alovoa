package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.urls.Url;
import com.linkedin.urls.detection.UrlDetector;
import com.linkedin.urls.detection.UrlDetectorOptions;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.ExceptionHandler;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.*;
import com.nonononoki.alovoa.lib.OxCaptcha;
import com.nonononoki.alovoa.model.*;
import com.nonononoki.alovoa.repo.*;
import com.sipgate.mp3wav.Converter;
import jakarta.mail.MessagingException;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserService.class);
    private static final int verificationStringLength = 5;
    private static final String MIME_X_WAV = "x-wav";
    private static final String MIME_WAV = "wav";
    private static final String MIME_MPEG = "mpeg";
    private static final String MIME_MP3 = "mp3";
    @Autowired
    private AuthService authService;
    @Autowired
    private MediaService mediaService;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private GenderRepository genderRepo;
    @Autowired
    private UserMiscInfoRepository userMiscInfoRepo;
    @Autowired
    private UserIntentionRepository userIntentionRepo;
    @Autowired
    private UserInterestRepository userInterestRepo;
    @Autowired
    private UserImageRepository userImageRepo;
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
    private UserVerificationPictureRepository userVerificationPictureRepo;
    @Autowired
    private ConversationRepository conversationRepo;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private MailService mailService;
    @Autowired
    private TextEncryptorConverter textEncryptor;
    @Autowired
    private ObjectMapper objectMapper;
    @Value("${app.age.min}")
    private int minAge;
    @Value("${app.age.max}")
    private int maxAge;
    @Value("${app.profile.image.length}")
    private int imageLength;
    @Value("${app.profile.image.max}")
    private int imageMax;
    @Value("${app.profile.description.size}")
    private int descriptionSize;
    @Value("${app.token.length}")
    private int tokenLength;
    @Value("${app.interest.max}")
    private int interestSize;
    @Value("${app.interest.min-chars}")
    private int interestMinCharSize;
    @Value("${app.interest.max-chars}")
    private int interestMaxCharSize;
    @Value("${app.interest.autocomplete.max}")
    private int interestAutocompleteMax;
    @Value("${app.audio.max-time}")
    private int audioMaxTime; // in seconds
    @Value("${app.user.delete.duration.valid}")
    private long userDeleteDuration;
    @Value("${app.intention.delay}")
    private long intentionDelay;
    @Value("${app.like.message.length}")
    private int likeMessageLength;
    @Value("${app.search.ignore-intention}")
    private boolean ignoreIntention;
    @Value("${app.domain}")
    public String domain;

    public String getDomain() {
        return domain;
    }


    public static void removeUserDataCascading(User user, UserDeleteParams userDeleteParam) {

        UserRepository userRepo = userDeleteParam.getUserRepo();
        UserLikeRepository userLikeRepo = userDeleteParam.getUserLikeRepo();
        ConversationRepository conversationRepo = userDeleteParam.getConversationRepo();
        UserNotificationRepository userNotificationRepo = userDeleteParam.getUserNotificationRepo();
        UserHideRepository userHideRepo = userDeleteParam.getUserHideRepo();
        UserBlockRepository userBlockRepo = userDeleteParam.getUserBlockRepo();
        UserReportRepository userReportRepo = userDeleteParam.getUserReportRepo();
        UserVerificationPictureRepository userVerificationPictureRepo = userDeleteParam.getUserVerificationPictureRepo();

        // DELETE USER LIKE
        for (UserLike like : userLikeRepo.findByUserFrom(user)) {
            User u = like.getUserTo();
            if (u != null && u.getLikedBy() != null) {
                u.getLikedBy().remove(like);
                userRepo.save(u);
            }
            like.setUserTo(null);
            userLikeRepo.save(like);

        }
        for (UserLike like : userLikeRepo.findByUserTo(user)) {
            User u = like.getUserFrom();
            if (u != null && u.getLikes() != null) {
                u.getLikes().remove(like);
                userRepo.save(u);
            }
            like.setUserFrom(null);
            userLikeRepo.save(like);
        }
        userRepo.flush();
        userLikeRepo.flush();

        // DELETE USER NOTIFICATION
        for (UserNotification notification : userNotificationRepo.findByUserFrom(user)) {
            User u = notification.getUserTo();
            if (u != null && u.getNotificationsFrom() != null) {
                u.getNotificationsFrom().remove(notification);
                userRepo.save(u);
            }
            notification.setUserTo(null);
            userNotificationRepo.save(notification);
        }
        for (UserNotification notificaton : userNotificationRepo.findByUserTo(user)) {
            User u = notificaton.getUserFrom();
            if (u != null && u.getNotifications() != null) {
                u.getNotifications().remove(notificaton);
                userRepo.save(u);
            }
            notificaton.setUserFrom(null);
            userNotificationRepo.save(notificaton);
        }
        userRepo.flush();
        userNotificationRepo.flush();

        // DELETE USER HIDE
        for (UserHide hide : userHideRepo.findByUserFrom(user)) {
            User u = hide.getUserTo();
            if (u != null && u.getHiddenByUsers() != null) {
                u.getHiddenByUsers().remove(hide);
                userRepo.save(u);
            }
            hide.setUserTo(null);
            userHideRepo.save(hide);
        }
        for (UserHide hide : userHideRepo.findByUserTo(user)) {
            User u = hide.getUserFrom();
            if (u != null && u.getHiddenUsers() != null) {
                u.getHiddenUsers().remove(hide);
                userRepo.save(u);
            }

            hide.setUserFrom(null);
            userHideRepo.save(hide);
        }
        userRepo.flush();
        userHideRepo.flush();

        // DELETE USER BLOCK
        for (UserBlock block : userBlockRepo.findByUserFrom(user)) {
            User u = block.getUserTo();
            if (u != null && u.getBlockedByUsers() != null) {
                u.getBlockedByUsers().remove(block);
                userRepo.save(u);
            }
            block.setUserTo(null);
            userBlockRepo.save(block);
        }
        for (UserBlock block : userBlockRepo.findByUserTo(user)) {
            User u = block.getUserFrom();
            if (u != null && u.getBlockedUsers() != null) {
                u.getBlockedUsers().remove(block);
                userRepo.save(u);
            }
            block.setUserFrom(null);
            userBlockRepo.save(block);
        }
        userRepo.flush();
        userBlockRepo.flush();

        // DELETE USER REPORT
        for (UserReport report : userReportRepo.findByUserFrom(user)) {
            User u = report.getUserTo();
            if (u != null && u.getReportedByUsers() != null) {
                u.getReportedByUsers().remove(report);
                userRepo.save(u);
            }
            report.setUserTo(null);
            userReportRepo.save(report);
        }
        for (UserReport report : userReportRepo.findByUserTo(user)) {
            User u = report.getUserFrom();
            if (u != null && u.getReported() != null) {
                u.getReported().remove(report);
                userRepo.save(u);
            }
            report.setUserFrom(null);
            userReportRepo.save(report);
        }
        userRepo.flush();
        userReportRepo.flush();

        // DELETE USER CONVERSATION
        for (Conversation c : conversationRepo.findByUsers_Id(user.getId())) {
            for (User u : c.getUsers()) {
                if (u != null && u.getConversations() != null) {
                    u.getConversations().remove(c);
                    userRepo.save(u);
                }
            }
            conversationRepo.delete(c);
        }

        // DELETE USER VERIFICATION
        for (UserVerificationPicture v : userVerificationPictureRepo.findByUserNo(user)) {
            v.getUserNo().remove(user);
            userVerificationPictureRepo.save(v);
        }
        for (UserVerificationPicture v : userVerificationPictureRepo.findByUserYes(user)) {
            v.getUserYes().remove(user);
            userVerificationPictureRepo.save(v);
        }

        userVerificationPictureRepo.flush();
        conversationRepo.flush();
        userRepo.flush();
    }

    public static String stripB64Type(String s) {
        if (s.contains(",")) {
            return s.split(",")[1];
        }
        return s;
    }

    private static byte[] convertAudioMp3Wav(byte[] bytes) {
        InputStream inputStream = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        final AudioFormat audioFormat = new AudioFormat(16000, 8, 1, false, false);
        Converter.convertFrom(inputStream).withTargetFormat(audioFormat).to(output);
        return output.toByteArray();
    }

    public UserDeleteToken deleteAccountRequest() throws MessagingException, IOException, AlovoaException {
        User user = authService.getCurrentUser(true);
        return deleteAccountRequestBase(user);
    }

    public UserDeleteToken deleteAccountRequestBase(User user) throws MessagingException, IOException, AlovoaException {
        UserDeleteToken token = new UserDeleteToken();
        Date currentDate = new Date();

        token.setContent(RandomStringUtils.random(tokenLength, 0, 0, true, true, null, new SecureRandom()));
        token.setDate(currentDate);
        token.setUser(user);
        if (user.getDeleteToken() != null) {
            token.setId(user.getDeleteToken().getId());
        }
        user.setDeleteToken(token);
        user = userRepo.saveAndFlush(user);

        mailService.sendAccountDeleteRequest(user);

        return user.getDeleteToken();
    }

    public void deleteAccountConfirm(UserDeleteAccountDto dto)
            throws MessagingException, IOException, AlovoaException, NoSuchAlgorithmException {
        User user = userRepo.findByEmail(Tools.cleanEmail(dto.getEmail()));

        if (user == null) {
            throw new AlovoaException(ExceptionHandler.USER_NOT_FOUND);
        }

        UserDeleteToken deleteToken = user.getDeleteToken();

        if (!dto.isConfirm() || deleteToken == null) {
            throw new AlovoaException("deletion_not_confirmed");
        }

        String userTokenString = deleteToken.getContent();

        long ms = new Date().getTime();
        if (ms - user.getDeleteToken().getDate().getTime() > userDeleteDuration) {
            throw new AlovoaException("deletion_not_valid");
        }

        if (!dto.getTokenString().equals(userTokenString)) {
            LOGGER.debug("Expected:" + userTokenString + ". Got: " + dto.getTokenString());
            throw new AlovoaException("deletion_wrong_token");
        }

        if (!captchaService.isValid(dto.getCaptchaId(), dto.getCaptchaText())) {
            throw new AlovoaException("captcha_invalid");
        }

        UserDeleteParams userDeleteParam = UserDeleteParams.builder().conversationRepo(conversationRepo)
                .userBlockRepo(userBlockRepo).userHideRepo(userHideRepo).userLikeRepo(userLikeRepo)
                .userNotificationRepo(userNotificationRepo).userRepo(userRepo).userReportRepo(userReportRepo)
                .userVerificationPictureRepo(userVerificationPictureRepo).build();

        removeUserDataCascading(user, userDeleteParam);

        user = userRepo.saveAndFlush(user);
        userRepo.delete(user);
        userRepo.flush();

        SecurityContextHolder.clearContext();
        mailService.sendAccountDeleteConfirm(user);
    }

    public void updateProfilePicture(byte[] bytes, String mimeType) throws AlovoaException, IOException {
        User user = authService.getCurrentUser(true);
        user.setVerificationPicture(null);
        AbstractMap.SimpleEntry<byte[], String> adjustedImage = adjustPicture(bytes, mimeType);
        if (user.getProfilePicture() == null) {
            UserProfilePicture profilePic = new UserProfilePicture();
            profilePic.setBin(adjustedImage.getKey());
            profilePic.setBinMime(adjustedImage.getValue());
            profilePic.setUser(user);
            user.setProfilePicture(profilePic);
        } else {
            user.getProfilePicture().setBin(adjustedImage.getKey());
            user.getProfilePicture().setBinMime(adjustedImage.getValue());
            user.getProfilePicture().setData(null);
        }

        userRepo.saveAndFlush(user);
    }

    public void onboarding(byte[] bytes, ProfileOnboardingDto model) throws AlovoaException, IOException {
        User user = authService.getCurrentUser(true);
        if (user.getProfilePicture() != null || user.getDescription() != null) {
            return;
        }

        Date now = new Date();

        UserProfilePicture profilePic = new UserProfilePicture();
        AbstractMap.SimpleEntry<byte[], String> adjustedImage = adjustPicture(bytes, model.getProfilePictureMime());
        profilePic.setBin(adjustedImage.getKey());
        profilePic.setBinMime(adjustedImage.getValue());
        user.setProfilePicture(profilePic);
        user.setVerificationPicture(null);

        UserIntention intention = userIntentionRepo.findById(model.getIntention()).orElse(null);
        user.getDates().setIntentionChangeDate(now);
        user.setIntention(intention);

        List<Gender> preferredGenders = genderRepo.findAllById(model.getPreferredGenders());
        user.setPreferedGenders(new HashSet<>(preferredGenders));

        user.setDescription(model.getDescription());
        user.getInterests().addAll((model.getInterests().stream().map(i -> {
            UserInterest interest = new UserInterest();
            interest.setText(i.toLowerCase());
            interest.setUser(user);
            return interest;
        }).toList()));

        userRepo.saveAndFlush(user);
    }

    public void updateDescription(String description) throws AlovoaException {
        if (description != null) {
            if (description.length() > descriptionSize) {
                throw new AlovoaException("max_length_exceeded");
            }
            if (description.trim().isEmpty()) {
                description = null;
            } else {
                UrlDetector parser = new UrlDetector(description, UrlDetectorOptions.Default);
                List<Url> urls = parser.detect();
                if (!urls.isEmpty()) {
                    throw new AlovoaException("url_detected");
                }
            }
        }
        User user = authService.getCurrentUser(true);
        user.setDescription(description);
        userRepo.saveAndFlush(user);
    }

    public void updateIntention(long intention) throws AlovoaException {
        User user = authService.getCurrentUser(true);

        Date now = new Date();
        if (user.getDates().getIntentionChangeDate() == null
                || now.getTime() >= user.getDates().getIntentionChangeDate().getTime() + intentionDelay) {
            boolean isLegal = Tools.calcUserAge(user) >= Tools.AGE_LEGAL;
            if (!isLegal && intention == UserIntention.SEX) {
                throw new AlovoaException("not_supported");
            }
            UserIntention i = userIntentionRepo.findById(intention).orElse(null);
            user.setIntention(i);
            user.getDates().setIntentionChangeDate(now);
            userRepo.saveAndFlush(user);
        } else {
            throw new AlovoaException("intention cooldown not finished");
        }
    }

    public void updateMinAge(int userMinAge) throws AlovoaException {
        if (userMinAge < minAge) {
            userMinAge = minAge;
        }
        User user = authService.getCurrentUser(true);
        user.setPreferedMinAge(userMinAge);
        userRepo.saveAndFlush(user);
    }

    public void updateMaxAge(int userMaxAge) throws AlovoaException {
        if (userMaxAge > maxAge) {
            userMaxAge = maxAge;
        }
        User user = authService.getCurrentUser(true);
        user.setPreferedMaxAge(userMaxAge);
        userRepo.saveAndFlush(user);
    }

    public void updatePreferedGender(long genderId, boolean activated) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        Set<Gender> list = user.getPreferedGenders();
        if (list == null) {
            list = new HashSet<>();
        }

        Gender g = genderRepo.findById(genderId).orElse(null);
        if (activated) {
            list.add(g);
        } else {
            list.remove(g);
        }
        user.setPreferedGenders(list);
        userRepo.saveAndFlush(user);
    }

    public Set<UserMiscInfo> updateUserMiscInfo(long infoValue, boolean activated) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        Set<UserMiscInfo> list = user.getMiscInfos();
        if (list == null) {
            list = new HashSet<>();
        }

        UserMiscInfo info = userMiscInfoRepo.findByValue(infoValue);
        if (activated) {
            list.add(info);

            if (infoValue >= UserMiscInfo.KIDS_NO && infoValue <= UserMiscInfo.KIDS_YES) {
                list.removeIf(o -> o.getValue() != infoValue && o.getValue() >= UserMiscInfo.KIDS_NO
                        && o.getValue() <= UserMiscInfo.KIDS_YES);
            } else if (infoValue >= UserMiscInfo.RELATIONSHIP_SINGLE && infoValue <= UserMiscInfo.RELATIONSHIP_OTHER) {
                list.removeIf(o -> o.getValue() != infoValue && o.getValue() >= UserMiscInfo.RELATIONSHIP_SINGLE
                        && o.getValue() <= UserMiscInfo.RELATIONSHIP_OTHER);
            }

        } else {
            list.remove(info);
        }
        user.setMiscInfos(list);
        userRepo.saveAndFlush(user);
        return list;
    }

    public void addInterest(String value) throws AlovoaException {
        User user = authService.getCurrentUser(true);

        if (value.length() < interestMinCharSize || value.length() > interestMaxCharSize
                || user.getInterests().size() >= interestSize) {
            throw new AlovoaException("interest_invalid_size");
        }

        Pattern pattern = Pattern.compile("[a-zA-Z0-9-]+");
        Matcher matcher = pattern.matcher(value);
        if (!matcher.matches()) {
            throw new AlovoaException("interest_unsupported_characters");
        }

        UserInterest interest = new UserInterest();
        interest.setText(value.toLowerCase());
        interest.setUser(user);

        if (user.getInterests() == null) {
            user.setInterests(new ArrayList<>());
        }

        if (user.getInterests().contains(interest)) {
            throw new AlovoaException("interest_already_exists");
        }

        user.getInterests().add(interest);

        userRepo.saveAndFlush(user);
    }

    public void deleteInterest(String interest) throws AlovoaException {
        User user = authService.getCurrentUser(true);

        Optional<UserInterest> interestOpt = user.getInterests().stream().filter(i -> i.getText().equals(interest))
                .findFirst();

        if (interest.isEmpty() || interestOpt.isEmpty()) {
            throw new AlovoaException("interest_does_not_exists");
        }

        user.getInterests().remove(interestOpt.get());
        userRepo.saveAndFlush(user);
    }

    public List<UserInterestDto> getInterestAutocomplete(String name) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        name = "%" + URLDecoder.decode(name, StandardCharsets.UTF_8) + "%";
        List<String> interestTexts = user.getInterests().stream().map(UserInterest::getText).collect(Collectors.toList());
        if (interestTexts.isEmpty()) {
            interestTexts.add("");
        }
        List<UserInterestDto> interests = userInterestRepo.getInterestAutocomplete(name, interestTexts,
                PageRequest.of(0, interestAutocompleteMax));
        interests.forEach(i -> i.setCountString(Tools.largeNumberToString(i.getCount())));
        return interests;
    }

    public void updateShowZodiac(int showZodiac) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        user.setShowZodiac(showZodiac == 1);
        userRepo.saveAndFlush(user);
    }

    public void updateUnits(int units) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        user.setUnits(units);
        userRepo.saveAndFlush(user);
    }

    public List<UserImageDto> addImage(byte[] bytes, String mimeType) throws AlovoaException, IOException {
        User user = authService.getCurrentUser(true);
        if (user.getImages() != null && user.getImages().size() < imageMax) {
            UserImage img = new UserImage();
            AbstractMap.SimpleEntry<byte[], String> adjustedImage = adjustPicture(bytes, mimeType);
            img.setBin(adjustedImage.getKey());
            img.setBinMime(adjustedImage.getValue());
            img.setDate(new Date());
            img.setUser(user);
            user.getImages().add(img);
            user = userRepo.saveAndFlush(user);
            return UserImageDto.buildFromUserImages(user, this);
        } else {
            throw new AlovoaException("max_image_amount_exceeded");
        }
    }

    public void deleteImage(long id) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        UserImage img = userImageRepo.findById(id).orElse(null);
        if (img != null && user.getImages().contains(img)) {
            user.getImages().remove(img);
            userRepo.saveAndFlush(user);
        }
    }

    private AbstractMap.SimpleEntry<byte[], String> adjustPicture(byte[] bytes, String mimeType) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        BufferedImage image = ImageIO.read(bis);

        if (image.getWidth() == imageLength && image.getHeight() == imageLength) {
            new AbstractMap.SimpleEntry<>(bytes, mimeType);
        }

        if (image.getWidth() != image.getHeight()) {

            int idealLength = image.getHeight();
            boolean heightShorter = true;
            if (image.getWidth() < image.getHeight()) {
                heightShorter = false;
                idealLength = image.getWidth();
            }

            // cut to a square
            int x = 0;
            int y = 0;

            if (heightShorter) {
                x = image.getWidth() / 2 - image.getHeight() / 2;
            } else {
                y = image.getHeight() / 2 - image.getWidth() / 2;
            }
            image = image.getSubimage(x, y, idealLength, idealLength);
        }

        // all images are equal in size
        BufferedImage scaledImage = new BufferedImage(imageLength, imageLength, image.getType());
        Graphics2D graphics2D = scaledImage.createGraphics();

        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        graphics2D.drawImage(image, 0, 0, imageLength, imageLength, null);
        graphics2D.dispose();
        image = scaledImage;

        // image to b64
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final String fileType = "webp";
        ImageIO.write(image, fileType, bos);
        return new AbstractMap.SimpleEntry<>(bos.toByteArray(), MediaService.MEDIA_TYPE_IMAGE_WEBP);
    }

    public void likeUser(UUID uuid, String message) throws AlovoaException {
        User user = findUserByUuid(uuid);
        User currUser = authService.getCurrentUser(true);

        if (user.equals(currUser)) {
            throw new AlovoaException("user_is_same_user");
        }

        if (user.getBlockedUsers().stream().filter(o -> o.getUserTo().getId() != null)
                .anyMatch(o -> o.getUserTo().getId().equals(currUser.getId()))) {
            throw new AlovoaException("is_blocked");
        }

        if (currUser.getBlockedUsers().stream().filter(o -> o.getUserTo().getId() != null)
                .anyMatch(o -> o.getUserTo().getId().equals(user.getId()))) {
            throw new AlovoaException("is_blocked");
        }

        if (!Tools.usersCompatible(currUser, user, ignoreIntention)) {
            throw new AlovoaException(ExceptionHandler.USER_NOT_COMPATIBLE);
        }

        if (message != null && message.length() > likeMessageLength) {
            throw new AlovoaException("max_length_exceeded");
        }

        if (userLikeRepo.findByUserFromAndUserTo(currUser, user) == null) {
            UserLike like = new UserLike();
            like.setDate(new Date());
            like.setUserFrom(currUser);
            like.setUserTo(user);
            currUser.getLikes().add(like);

            UserNotification not = new UserNotification();
            not.setContent(UserNotification.USER_LIKE);
            not.setDate(new Date());
            not.setUserFrom(currUser);
            not.setUserTo(user);
            not.setMessage(message);
            currUser.getNotifications().add(not);

            user.getDates().setNotificationDate(new Date());

            currUser.getHiddenUsers().removeIf(hide -> hide.getUserTo().getId().equals(user.getId()));

            userRepo.saveAndFlush(user);
            userRepo.saveAndFlush(currUser);

            final boolean isMatch = user.getLikes().stream().filter(o -> o.getUserTo().getId() != null)
                    .anyMatch(o -> o.getUserTo().getId().equals(currUser.getId()));

            if (isMatch) {
                Conversation convo = new Conversation();
                convo.setUsers(new ArrayList<>());
                convo.setDate(new Date());
                convo.getUsers().add(currUser);
                convo.getUsers().add(user);
                convo.setLastUpdated(new Date());
                convo.setMessages(new ArrayList<>());
                conversationRepo.saveAndFlush(convo);

                user.getConversations().add(convo);
                currUser.getConversations().add(convo);

                userRepo.saveAndFlush(currUser);
                userRepo.saveAndFlush(user);

                if (user.getUserSettings().isEmailLike()) {
                    mailService.sendMatchNotificationMail(user);
                }

            } else if (user.getUserSettings().isEmailLike()) {
                mailService.sendLikeNotificationMail(user);
            }
        }
    }

    public void hideUser(UUID uuid)
            throws NumberFormatException, AlovoaException {
        User user = findUserByUuid(uuid);
        User currUser = authService.getCurrentUser(true);
        if (userHideRepo.findByUserFromAndUserTo(currUser, user) == null) {
            UserHide hide = new UserHide();
            hide.setDate(new Date());
            hide.setUserFrom(currUser);
            hide.setUserTo(user);
            currUser.getHiddenUsers().add(hide);
            userRepo.saveAndFlush(currUser);

            if(user.getLikes().stream().anyMatch(l -> l.getUserTo().getId().equals(currUser.getId()))) {
                blockUser(uuid);
            }
        }
    }

    public void blockUser(UUID uuid)
            throws AlovoaException, NumberFormatException {
        User user = findUserByUuid(uuid);
        User currUser = authService.getCurrentUser(true);
        if (userBlockRepo.findByUserFromAndUserTo(currUser, user) == null) {
            UserBlock block = new UserBlock();
            block.setDate(new Date());
            block.setUserFrom(currUser);
            block.setUserTo(user);
            currUser.getBlockedUsers().add(block);
            userRepo.saveAndFlush(currUser);
        }
    }

    public void unblockUser(UUID uuid)
            throws AlovoaException, NumberFormatException {
        User user = findUserByUuid(uuid);
        User currUser = authService.getCurrentUser(true);

        UserBlock block = userBlockRepo.findByUserFromAndUserTo(currUser, user);
        if (block != null) {
            currUser.getBlockedUsers().remove(block);
            userRepo.save(currUser);
        }
    }

    public UserReport reportUser(UUID uuid, String comment)
            throws AlovoaException, NumberFormatException {
        User user = findUserByUuid(uuid);
        User currUser = authService.getCurrentUser(true);
        if (userReportRepo.findByUserFromAndUserTo(currUser, user) == null) {
            UserReport report = new UserReport();
            report.setDate(new Date());
            report.setUserFrom(currUser);
            report.setUserTo(user);
            report.setComment(comment);
            currUser.getReported().add(report);
            currUser = userRepo.saveAndFlush(currUser);

            return currUser.getReported().get(currUser.getReported().size() - 1);
        }

        return null;
    }

    public boolean hasNewAlert() throws AlovoaException {
        return hasNewAlert(null);
    }

    public boolean hasNewAlert(String lang) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        // user always check their alerts periodically in the background, so just update
        // it here
        if (user != null) {
            updateUserInfo(user, lang);
            return user.getDates().getNotificationDate().after(user.getDates().getNotificationCheckedDate());
        } else {
            return false;
        }
    }

    public boolean hasNewMessage() throws AlovoaException {
        User user = authService.getCurrentUser(true);
        if (user != null && user.getDates().getMessageDate() != null
                && user.getDates().getMessageCheckedDate() != null) {
            return user.getDates().getMessageDate().after(user.getDates().getMessageCheckedDate());
        } else {
            return false;
        }
    }

    public void updateUserInfo(User user) {
        updateUserInfo(user, null);
    }

    public void updateUserInfo(User user, String language) {
        if (user != null && user.getDates() != null) {
            user.getDates().setActiveDate(new Date());
            String lang;
            if (language == null) {
                lang = LocaleContextHolder.getLocale().getLanguage();
            } else {
                lang = language;
            }
            if (!lang.equals(user.getLanguage())) {
                user.setLanguage(lang);
            }
            userRepo.saveAndFlush(user);
        }
    }

    public ResponseEntity<Resource> getUserdata(UUID uuid) throws AlovoaException, JsonProcessingException,
            NumberFormatException {
        User user = authService.getCurrentUser(true);
        User userFromUuid = findUserByUuid(uuid);
        if (!user.getId().equals(userFromUuid.getId())) {
            throw new AlovoaException("users_not_equal");
        }

        UserGdpr ug = UserGdpr.userToUserGdpr(user);
        String json = objectMapper.writeValueAsString(ug);
        ByteArrayResource resource = new ByteArrayResource(json.getBytes(StandardCharsets.UTF_8));

        ContentDisposition contentDisposition = ContentDisposition.builder("inline").filename("alovoa_userdata.json")
                .build();

        MediaType mediaType = MediaTypeFactory.getMediaType(resource).orElse(MediaType.APPLICATION_OCTET_STREAM);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(contentDisposition);

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    public String getAudio(UUID uuid)
            throws NumberFormatException, AlovoaException {
        User user = findUserByUuid(uuid);
        if (user.getAudio() == null) {
            return null;
        }
        return user.getAudio().getData();
    }

    public void deleteAudio() throws AlovoaException {
        User user = authService.getCurrentUser(true);
        user.setAudio(null);
        userRepo.saveAndFlush(user);
    }

    public void updateAudio(byte[] bytes, String mimeType)
            throws AlovoaException, UnsupportedAudioFileException, IOException {
        User user = authService.getCurrentUser(true);
        byte[] newAudioB64 = adjustAudio(bytes, mimeType);
        if (user.getAudio() == null) {
            UserAudio audio = new UserAudio();
            audio.setBin(newAudioB64);
            audio.setUser(user);
            user.setAudio(audio);
        } else {
            user.getAudio().setBin(newAudioB64);
        }
        userRepo.saveAndFlush(user);
    }

    public void updateCountry(String countryIso) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        boolean validCountryIso = Arrays.asList(Locale.getISOCountries()).contains(countryIso);
        if (validCountryIso) {
            user.setCountry(countryIso);
        } else {
            // remove flag if user is not in a valid country
            user.setCountry(null);
        }

        userRepo.saveAndFlush(user);
    }

    public String getVerificationCode() throws AlovoaException {
        User user = authService.getCurrentUser(true);
        return getVerificationCode(user);
    }

    public String getVerificationCode(User user) {
        if (user.getVerificationCode() != null) {
            return user.getVerificationCode();
        } else {
            String verificationString = new String(OxCaptcha.genText(verificationStringLength));
            user.setVerificationCode(verificationString);
            userRepo.saveAndFlush(user);
            return verificationString;
        }
    }

    public void updateVerificationPicture(byte[] bytes, String mimeType) throws AlovoaException, IOException {
        User user = authService.getCurrentUser(true);

        //verification picture only usable with profile picture
        if (user.getProfilePicture() == null) {
            throw new AlovoaException("need_profile_picture");
        }
        user.setVerificationPicture(null);
        userRepo.saveAndFlush(user);
        AbstractMap.SimpleEntry<byte[], String> adjustedImage = adjustPicture(bytes, mimeType);
        UserVerificationPicture verificationPicture = new UserVerificationPicture();
        verificationPicture.setBin(adjustedImage.getKey());
        verificationPicture.setBinMime(adjustedImage.getValue());
        verificationPicture.setDate(new Date());
        verificationPicture.setUser(user);
        verificationPicture.setUserNo(new ArrayList<>());
        verificationPicture.setUserYes(new ArrayList<>());
        user.setVerificationPicture(verificationPicture);
        userRepo.saveAndFlush(user);
    }

    public void upvoteVerificationPicture(UUID uuid) throws AlovoaException, IOException, InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        User currentUser = authService.getCurrentUser(true);
        User user = findUserByUuid(uuid);

        if (currentUser.equals(user)) {
            throw new AlovoaException("invalid_user");
        }

        if (user.getVerificationPicture() == null) {
            throw new AlovoaException("no_verification_picture");
        }

        if (user.getVerificationPicture().getUserYes().contains(currentUser) || user.getVerificationPicture().getUserNo().contains(currentUser)) {
            throw new AlovoaException("already_voted");
        }

        user.getVerificationPicture().getUserYes().add(currentUser);
        userRepo.saveAndFlush(user);
    }

    public void downvoteVerificationPicture(UUID uuid) throws AlovoaException {
        User currentUser = authService.getCurrentUser(true);
        User user = findUserByUuid(uuid);

        if (currentUser.equals(user)) {
            throw new AlovoaException("invalid_user");
        }

        if (user.getVerificationPicture() == null) {
            throw new AlovoaException("no_verification_picture");
        }

        if (user.getVerificationPicture().getUserYes().contains(currentUser) || user.getVerificationPicture().getUserNo().contains(currentUser)) {
            throw new AlovoaException("already_voted");
        }

        user.getVerificationPicture().getUserNo().add(currentUser);
        userRepo.saveAndFlush(user);
    }

    public void updateUUID(User user, UUID uuid) {
        if(user.getUuid() == null) {
            user.setUuid(uuid);
            userRepo.saveAndFlush(user);
        }
    }

    public void updateImageUUID(UserImage image, UUID uuid) {
        if(image.getUuid() == null) {
            image.setUuid(uuid);
            userImageRepo.saveAndFlush(image);
        }
    }

    public User findUserByUuid(UUID uuid) throws AlovoaException {
        User user = userRepo.findByUuid(uuid);
        if(user == null) {
            throw new AlovoaException("user_not_found: " + uuid);
        }
        return user;
    }

    private byte[] adjustAudio(byte[] bytes, String mimeType) throws UnsupportedAudioFileException, IOException {
        if (mimeType.equals(MIME_X_WAV) || mimeType.equals(MIME_WAV)) {
            return trimAudioWav(bytes, audioMaxTime);
        } else if (mimeType.equals(MIME_MPEG) || mimeType.equals(MIME_MP3)) {
            return trimAudioWav(convertAudioMp3Wav(bytes), audioMaxTime);
        }
        return null;
    }

    private byte[] trimAudioWav(byte[] bytes, int maxSeconds) throws UnsupportedAudioFileException, IOException {

        ByteArrayInputStream bis;
        ByteArrayOutputStream baos;
        AudioInputStream ais = null;
        AudioInputStream aisShort = null;

        try {
            bis = new ByteArrayInputStream(bytes);
            ais = AudioSystem.getAudioInputStream(bis);
            AudioFormat format = ais.getFormat();
            long frameLength = ais.getFrameLength();

            // check if audio is shorter or equal max length
            double durationInSeconds = frameLength * format.getFrameRate();
            if (durationInSeconds < 0.0) {
                durationInSeconds = durationInSeconds * (-1);
            }
            if (durationInSeconds <= maxSeconds) {
                ais.close();
                return bytes;
            } else {
                long frames = (long) (format.getFrameRate() * maxSeconds);
                aisShort = new AudioInputStream(ais, format, frames);
                AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;
                baos = new ByteArrayOutputStream();
                AudioSystem.write(aisShort, targetType, baos);
                aisShort.close();
                ais.close();
                return baos.toByteArray();
            }
        } catch (Exception e) {
            if (ais != null) {
                ais.close();
            }
            if (aisShort != null) {
                aisShort.close();
            }
            throw e;
        }
    }
}
