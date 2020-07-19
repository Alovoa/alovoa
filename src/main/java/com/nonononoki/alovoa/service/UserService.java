package com.nonononoki.alovoa.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Conversation;
import com.nonononoki.alovoa.entity.Gender;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserBlock;
import com.nonononoki.alovoa.entity.UserDeleteToken;
import com.nonononoki.alovoa.entity.UserHide;
import com.nonononoki.alovoa.entity.UserImage;
import com.nonononoki.alovoa.entity.UserIntention;
import com.nonononoki.alovoa.entity.UserInterest;
import com.nonononoki.alovoa.entity.UserLike;
import com.nonononoki.alovoa.entity.UserNotification;
import com.nonononoki.alovoa.entity.UserReport;
import com.nonononoki.alovoa.model.UserDeleteAccountDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserBlockRepository;
import com.nonononoki.alovoa.repo.UserDeleteTokenRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserImageRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.repo.UserInterestRepository;
import com.nonononoki.alovoa.repo.UserLikeRepository;
import com.nonononoki.alovoa.repo.UserNotificationRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class UserService {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private GenderRepository genderRepo;

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
	private ConversationRepository conversationRepo;

	@Autowired
	private UserDeleteTokenRepository userDeleteTokenRepo;

	@Autowired
	private CaptchaService captchaService;

	@Autowired
	private MailService mailService;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private PasswordEncoder passwordEncoder;

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

	@Autowired
	private TextEncryptorConverter textEncryptor;

	public void deleteAccountRequest(String password) throws MessagingException {
		User user = authService.getCurrentUser();
		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new BadCredentialsException("");
		}

		UserDeleteToken token = null;

		if (user.getDeleteToken() != null) {
//			userDeleteTokenRepo.delete(user.getDeleteToken());
			token = user.getDeleteToken();
		} else {
			token = new UserDeleteToken();
		}

		token.setContent(RandomStringUtils.randomAlphanumeric(tokenLength));
		token.setDate(new Date());
		token.setUser(user);
		userDeleteTokenRepo.save(token);

		user.setDeleteToken(token);
		userRepo.save(user);

		mailService.sendAccountDeleteRequest(user, token);
	}

	public void deleteAccountConfirm(UserDeleteAccountDto dto) throws Exception {
		User user = authService.getCurrentUser();
		String userTokenString = user.getDeleteToken().getContent();

		if (!dto.isConfirm()) {
			throw new Exception("");
		}

		if (!dto.getTokenString().equals(userTokenString)) {
			throw new Exception("");
		}

		if (!dto.getEmail().equals(user.getEmail())) {
			throw new Exception("");
		}

		if (!captchaService.isValid(dto.getCaptchaId(), dto.getCaptchaText())) {
			throw new Exception("");
		}

		conversationRepo.deleteAll(conversationRepo.findAllByUserFrom(user));
		conversationRepo.deleteAll(conversationRepo.findAllByUserTo(user));
		userRepo.delete(user);
		mailService.sendAccountDeleteConfirm(user);
	}

	public void updateProfilePicture(String imgB64) throws Exception {
		User user = authService.getCurrentUser();
		String newImgB64 = adjustPicture(imgB64);
		user.setProfilePicture(newImgB64);
		userRepo.save(user);
	}

	public void updateDescription(String description) throws Exception {
		if (description.length() > descriptionSize) {
			throw new Exception("");
		}
		User user = authService.getCurrentUser();
		user.setDescription(description);
		userRepo.save(user);
	}

	public void updateIntention(long intention) {
		// TODO Limit intention changing time
		User user = authService.getCurrentUser();
		UserIntention i = userIntentionRepo.findById(intention).orElse(null);
		user.setIntention(i);
		user.getDates().setIntentionChangeDate(new Date());
		userRepo.save(user);
	}

	public void updateMinAge(int userMinAge) throws Exception {
		if (userMinAge < minAge) {
			userMinAge = minAge;
		}
		User user = authService.getCurrentUser();
		user.setPreferedMinAge(userMinAge);
		userRepo.save(user);
	}

	public void updateMaxAge(int userMaxAge) throws Exception {
		if (userMaxAge > maxAge) {
			userMaxAge = maxAge;
		}
		User user = authService.getCurrentUser();
		user.setPreferedMaxAge(userMaxAge);
		userRepo.save(user);
	}

	public void updatePreferedGender(long genderId, boolean activated) {
		User user = authService.getCurrentUser();
		Set<Gender> list = user.getPreferedGenders();
		if(list == null) {
			list = new HashSet<Gender>();
		}
		
		Gender g = genderRepo.findById(genderId).orElse(null);
		if (activated) {
			if (!list.contains(g)) {
				list.add(g);
			}
		} else {
			if (list.contains(g)) {
				list.remove(g);
			}
		}
		user.setPreferedGenders(list);
		userRepo.save(user);
	}

	public void addInterest(String value) throws Exception {
		User user = authService.getCurrentUser();

		if (value.length() < interestMinCharSize || value.length() > interestMaxCharSize
				|| user.getInterests().size() >= interestSize) {
			throw new Exception("");
		}
		
		Pattern pattern = Pattern.compile("[a-zA-Z0-9-]+");
		Matcher matcher = pattern.matcher(value);
		if(!matcher.matches()) {
			throw new Exception("");
		}		

		UserInterest interest = new UserInterest();
		interest.setText(value.toLowerCase());
		interest.setUser(user);
		
		if(user.getInterests().contains(interest)) {
			throw new Exception("");
		}
		
		userInterestRepo.save(interest);
	}

	public void deleteInterest(long interestId) throws Exception {
		User user = authService.getCurrentUser();
		UserInterest interest = userInterestRepo.findById(interestId).orElse(null);

		if (interest == null) {
			throw new Exception("");
		}

		if (!user.getInterests().contains(interest)) {
			throw new Exception("");
		}

		//userInterestRepo.delete(interest);
		user.getInterests().remove(interest);
		userRepo.save(user);
	}

	public void updateTheme(int themeId) {
		User user = authService.getCurrentUser();
		user.setTheme(themeId);
		userRepo.save(user);
	}
	
	public void addImage(String imgB64) throws Exception {
		User user = authService.getCurrentUser();
		if(user.getImages() != null && 
				user.getImages().size() < imageMax) {
			
			UserImage img = new UserImage();
			img.setContent(adjustPicture(imgB64));
			img.setDate(new Date());
			img.setUser(user);
			user.getImages().add(img);
			userRepo.save(user);
		} else {
			throw new Exception("");
		}
	}
	
	public void deleteImage(long id) {
		User user = authService.getCurrentUser();
		UserImage img = userImageRepo.getOne(id);
		
		if(user.getImages().contains(img)) {
			user.getImages().remove(img);
			userRepo.save(user);
		}
	}

	private String adjustPicture(String imgB64) throws IOException {
		// convert b64 to bufferedimage
		BufferedImage image = null;
		byte[] decodedBytes = Base64.getDecoder().decode(stripImageType(imgB64));
		ByteArrayInputStream bis = new ByteArrayInputStream(decodedBytes);
		image = ImageIO.read(bis);

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
				y = 0;
				x = image.getWidth() / 2 - image.getHeight() / 2;
			} else {
				x = 0;
				y = image.getHeight() / 2 - image.getWidth() / 2;
			}
			image = image.getSubimage(x, y, idealLength, idealLength);
		}

		if (image.getWidth() > imageLength) {
			// scale image if it's too big
			BufferedImage scaledImage = new BufferedImage(imageLength, imageLength, image.getType());
			Graphics2D graphics2D = scaledImage.createGraphics();

			// chose one
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			// graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING,
			// RenderingHints.VALUE_RENDER_QUALITY);
			// graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
			// RenderingHints.VALUE_ANTIALIAS_ON);

			graphics2D.drawImage(image, 0, 0, imageLength, imageLength, null);
			graphics2D.dispose();
			image = scaledImage;
		}

		// image to b64
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		byte[] bytes = out.toByteArray();
		String base64bytes = Base64.getEncoder().encodeToString(bytes);
		String src = Tools.B64IMAGEPREFIX + base64bytes;
		return src;

	}

	private String stripImageType(String s) {
		if (s.contains(",")) {
			return s.split(",")[1];
		}
		return s;
	}

	public void likeUser(String idEnc) throws NumberFormatException, Exception {
		User user = encodedIdToUser(idEnc);
		User currUser = authService.getCurrentUser();

		if (user.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(currUser.getId()))) {
			throw new Exception();
		}
		if (userLikeRepo.findByUserFromAndUserTo(currUser, user) == null) {
			UserLike like = new UserLike();
			like.setDate(new Date());
			like.setUserFrom(currUser);
			like.setUserTo(user);
			userLikeRepo.save(like);

			UserNotification not = new UserNotification();
			not.setContent(not.getUSER_LIKE());
			not.setCreationDate(new Date());
			not.setUserFrom(currUser);
			not.setUserTo(user);
			userNotificationRepo.save(not);
			notificationService.newLike(user);

			if (user.getLikes().stream().anyMatch(o -> o.getUserTo().getId().equals(currUser.getId()))) {
				Conversation convo = new Conversation();
				convo.setCreationDate(new Date());
				convo.setUserFrom(currUser);
				convo.setUserTo(user);
				convo.setLastUpdated(new Date());
				conversationRepo.save(convo);
//				user.getDates().setMessageDate(new Date());
//				userRepo.saveAndFlush(user);

				notificationService.newMatch(user);
				notificationService.newMatch(currUser);
			}

			user.getDates().setNotificationDate(new Date());
			userRepo.saveAndFlush(user);
		}
	}

	public void hideUser(String idEnc) throws NumberFormatException, Exception {
		User user = encodedIdToUser(idEnc);
		User currUser = authService.getCurrentUser();
		if (userHideRepo.findByUserFromAndUserTo(currUser, user) == null) {
			UserHide hide = new UserHide();
			hide.setDate(new Date());
			hide.setUserFrom(currUser);
			hide.setUserTo(user);
			userHideRepo.save(hide);
		}
	}

	public void blockUser(String idEnc) throws NumberFormatException, Exception {
		User user = encodedIdToUser(idEnc);
		User currUser = authService.getCurrentUser();
		if (userBlockRepo.findByUserFromAndUserTo(currUser, user) == null) {
			UserBlock block = new UserBlock();
			block.setDate(new Date());
			block.setUserFrom(currUser);
			block.setUserTo(user);
			userBlockRepo.save(block);
		}
	}

	public void unblockUser(String idEnc) throws NumberFormatException, Exception {
		User user = encodedIdToUser(idEnc);
		User currUser = authService.getCurrentUser();

		UserBlock block = userBlockRepo.findByUserFromAndUserTo(currUser, user);
		if (block != null) {
			//userBlockRepo.delete(block);
			currUser.getBlockedUsers().remove(block);
			userRepo.save(currUser);
		}
	}

	public void reportUser(String idEnc, long captchaId, String captchaText, String comment) throws NumberFormatException, Exception {
		User user = encodedIdToUser(idEnc);
		User currUser = authService.getCurrentUser();
		if (userReportRepo.findByUserFromAndUserTo(currUser, user) == null) {

			boolean isValid = captchaService.isValid(captchaId, captchaText);
			if (!isValid) {
				throw new Exception("");
			}
			UserReport report = new UserReport();
			report.setDate(new Date());
			report.setUserFrom(currUser);
			report.setUserTo(user);
			report.setComment(comment);
			userReportRepo.save(report);
		}
	}

	public User encodedIdToUser(String idEnc) throws NumberFormatException, Exception {
		long id = Long.parseLong(textEncryptor.decode(idEnc));
		User user = userRepo.findById(id).orElse(null);
		return user;
	}

	public boolean hasNewAlert() {
		User currUser = authService.getCurrentUser();
		// user always check their alerts periodically in the background, so just update
		// it here
		updateActiveDate(currUser);
		return currUser.getDates().getNotificationDate().after(currUser.getDates().getNotificationCheckedDate());
	}

	public boolean hasNewMessage() {
		User currUser = authService.getCurrentUser();
		return currUser.getDates().getMessageDate().after(currUser.getDates().getMessageCheckedDate());
	}

	private void updateActiveDate(User user) {
		user.getDates().setActiveDate(new Date());
		userRepo.save(user);
	}

	public void getUserdata(String password) throws Exception {
		User user = authService.getCurrentUser();
		if (!passwordEncoder.matches(password, user.getPassword())) {
			throw new BadCredentialsException("");
		}
		mailService.sendUserDataMail(user);
	}
}
