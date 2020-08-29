package com.nonononoki.alovoa.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

import javax.mail.MessagingException;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserDates;
import com.nonononoki.alovoa.entity.UserRegisterToken;
import com.nonononoki.alovoa.model.BaseRegisterDto;
import com.nonononoki.alovoa.model.RegisterDto;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.repo.UserRegisterTokenRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class RegisterService {

	private final String TEMP_EMAIL_FILE_NAME = "temp-mail.txt";

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

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private MailService mailService;

	@Autowired
	private PublicService publicService;

	@Autowired
	private UserRepository userRepo;

	//@Autowired
	//private UserDatesRepository userDatesRepo;

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
	private TextEncryptorConverter textEncryptor;

	//@Autowired
	//private HttpServletRequest request;

	public String register(RegisterDto dto) throws Exception {
		
		boolean isValid = captchaService.isValid(dto.getCaptchaId(), dto.getCaptchaText());
		if (!isValid) {
			throw new Exception(publicService.text("backend.error.captcha.invalid"));
		}

		User user = userRepo.findByEmail(dto.getEmail().toLowerCase());
		if (user != null) {
			throw new Exception(publicService.text("backend.error.register.email-exists"));
		}

		BaseRegisterDto baseRegisterDto = registerBase(dto);
		user = baseRegisterDto.getUser();

		// check if email is in spam mail list
		if (profile.equals("prod")) {
			try {
				if (Tools.isTextContainingLineFromFile(Tools.getFileFromResources(TEMP_EMAIL_FILE_NAME),
						user.getEmail())) {
					throw new Exception(publicService.text("backend.error.register.email-spam"));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		user.setPassword(passwordEncoder.encode(dto.getPassword()));
		user = userRepo.saveAndFlush(user);

		UserRegisterToken token = createUserToken(user);
		return token.getContent();
	}

	public void registerOauth(RegisterDto dto) throws Exception {

		String email = textEncryptor.decode(dto.getEmail());
		
		if(!email.equals(authService.getOauth2Email())) {
			throw new Exception("");
		}
		
		User user = userRepo.findByEmail(email);
		if (user != null) {
			throw new Exception(publicService.text("backend.error.register.email-exists"));
		}
		
		dto.setEmail(email);
		BaseRegisterDto baseRegisterDto = registerBase(dto);
		user = baseRegisterDto.getUser();
		user.setConfirmed(true);
		userRepo.saveAndFlush(user);
	}

	public UserRegisterToken createUserToken(User user) throws MessagingException {
		UserRegisterToken token = generateToken(user);
		user.setRegisterToken(token);
		user = userRepo.saveAndFlush(user);
		mailService.sendRegistrationMail(user, token);
		return token;
	}

	public UserRegisterToken generateToken(User user) {
		UserRegisterToken token = new UserRegisterToken();
		token.setContent(RandomStringUtils.randomAlphanumeric(tokenLength));
		token.setCreationDate(new Date());
		token.setUser(user);
		return registerTokenRepo.saveAndFlush(token);
	}

	public User registerConfirm(String tokenString) throws Exception {
		UserRegisterToken token = registerTokenRepo.findByContent(tokenString);

		if (token == null) {
			throw new Exception();
		}

		User user = token.getUser();

		if (user == null) {
			throw new Exception();
		}

		if (user.isConfirmed()) {
			throw new Exception();
		}

		user.setConfirmed(true);
		user.setRegisterToken(null);
		return userRepo.saveAndFlush(user);
	}

	private BaseRegisterDto registerBase(RegisterDto dto) throws Exception {
		// check minimum age
		LocalDate now = LocalDate.now();
		Period period = Period.between(
				dto.getDateOfBirth().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(), now);
		int userAge = period.getYears();
		if (userAge < minAge) {
			throw new Exception(publicService.text("backend.error.register.min-age"));
		}

		User user = new User();
		user.setEmail(dto.getEmail().toLowerCase());
		user.setFirstName(dto.getFirstName());
		int userMinAge = userAge - ageRange;
		int userMaxAge = userAge + ageRange;
		if (userMinAge < minAge) {
			userMinAge = minAge;
		}
		if (userMaxAge > maxAge) {
			userMaxAge = maxAge;
		}
		// user.setAge(userAge);
		user.setPreferedMinAge(userMinAge);
		user.setPreferedMaxAge(userMaxAge);
		user.setGender(genderRepo.findById(dto.getGender()).orElse(null));
		if (user.getGender() == null) {
			throw new Exception("");
		}
		user.setIntention(userIntentionRepo.findById(dto.getIntention()).orElse(null));

//				user = userRepo.saveAndFlush(user);
		UserDates dates = new UserDates();
		Date today = new Date();
		dates.setActiveDate(today);
		dates.setCreationDate(today);
		dates.setDateOfBirth(dto.getDateOfBirth().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
		dates.setIntentionChangeDate(today);
		dates.setMessageCheckedDate(today);
		dates.setMessageDate(today);
		dates.setNotificationCheckedDate(today);
		dates.setNotificationDate(today);
		dates.setUser(user);
//				dates = userDatesRepo.saveAndFlush(dates);
		user.setDates(dates);

		// resolves hibernate issue with null Collections with orphanremoval
		// https://hibernate.atlassian.net/browse/HHH-9940
		user.setInterests(new ArrayList());
		user.setImages(new ArrayList());
		user.setDonations(new ArrayList());
		user.setLikes(new ArrayList());
		user.setLikedBy(new ArrayList());
		user.setConversations(new ArrayList());
		user.setConversationsBy(new ArrayList());
		user.setMessageReceived(new ArrayList());
		user.setMessageSent(new ArrayList());
		user.setNotifications(new ArrayList());
		user.setNotificationsFrom(new ArrayList());
		user.setHiddenByUsers(new ArrayList());
		user.setHiddenUsers(new ArrayList());
		user.setBlockedByUsers(new ArrayList());
		user.setBlockedUsers(new ArrayList());
		user.setReported(new ArrayList());
		user.setReportedByUsers(new ArrayList());
		user.setWebPush(new ArrayList());
		
		user = userRepo.saveAndFlush(user);

		BaseRegisterDto baseRegisterDto = new BaseRegisterDto();
		baseRegisterDto.setRegisterDto(dto);
		baseRegisterDto.setUser(user);
		return baseRegisterDto;
	}
}
