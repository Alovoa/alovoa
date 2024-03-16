package com.nonononoki.alovoa.config;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.entity.user.UserIntention;
import com.nonononoki.alovoa.entity.user.UserMiscInfo;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDeleteParams;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserBlockRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.repo.UserLikeRepository;
import com.nonononoki.alovoa.repo.UserMiscInfoRepository;
import com.nonononoki.alovoa.repo.UserNotificationRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.UserService;

@Component
public class EventListenerConfig {

	@Autowired
	private UserRepository userRepo;

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
	private GenderRepository genderRepo;
	
	@Autowired
	private UserMiscInfoRepository userMiscInfoRepo;

	@Autowired
	private UserIntentionRepository userIntentionRepo;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${app.admin.email}")
	private String adminEmail;

	@Value("${app.admin.key}")
	private String adminKey;

	private static final Logger logger = LoggerFactory.getLogger(EventListenerConfig.class);

	@EventListener
	public void handleContextRefresh(ApplicationStartedEvent event) {
		setDefaultAdmin();
		setDefaultGenders();
		setDefaultIntentions();
		setDefaultUserMiscInfo();
	}

	private void setDefaultUserMiscInfo() {
		if(userMiscInfoRepo.count() == 0) {
//			public static final long DRUGS_TOBACCO = 1;
//			@Transient
//			public static final long DRUGS_ALCOHOL = 2;
//			@Transient
//			public static final long DRUGS_CANNABIS = 3;
//			@Transient
//			public static final long DRUGS_OTHER = 4;
//			
//			@Transient
//			public static final long RELATIONSHIP_SINGLE = 11;
//			@Transient
//			public static final long RELATIONSHIP_TAKEN = 12;
//			@Transient
//			public static final long RELATIONSHIP_OPEN = 13;
//			@Transient
//			public static final long RELATIONSHIP_OTHER = 14;
//			
//			@Transient
//			public static final long KIDS_NO = 21;
//			@Transient
//			public static final long KIDS_YES = 22;
			
			UserMiscInfo drugsTobaccoInfo = new UserMiscInfo();
			drugsTobaccoInfo.setValue(UserMiscInfo.DRUGS_TOBACCO);
			
			UserMiscInfo drugsAlcoholInfo = new UserMiscInfo();
			drugsAlcoholInfo.setValue(UserMiscInfo.DRUGS_ALCOHOL);
			
			UserMiscInfo drugsCannabisInfo = new UserMiscInfo();
			drugsCannabisInfo.setValue(UserMiscInfo.DRUGS_CANNABIS);
			
			UserMiscInfo drugsOtherInfo = new UserMiscInfo();
			drugsOtherInfo.setValue(UserMiscInfo.DRUGS_OTHER);
			
			
			UserMiscInfo relationshipSingleInfo = new UserMiscInfo();
			relationshipSingleInfo.setValue(UserMiscInfo.RELATIONSHIP_SINGLE);
			
			UserMiscInfo relationshipTakenInfo = new UserMiscInfo();
			relationshipTakenInfo.setValue(UserMiscInfo.RELATIONSHIP_TAKEN);
						
			UserMiscInfo relationshipOpenInfo = new UserMiscInfo();
			relationshipOpenInfo.setValue(UserMiscInfo.RELATIONSHIP_OPEN);
			
			UserMiscInfo relationshipOtherInfo = new UserMiscInfo();
			relationshipOtherInfo.setValue(UserMiscInfo.RELATIONSHIP_OTHER);
			
			
			UserMiscInfo kidsNoInfo = new UserMiscInfo();
			kidsNoInfo.setValue(UserMiscInfo.KIDS_NO);
			
			UserMiscInfo kidsYesInfo = new UserMiscInfo();
			kidsYesInfo.setValue(UserMiscInfo.KIDS_YES);
			
			userMiscInfoRepo.saveAllAndFlush(Arrays.asList(
					drugsTobaccoInfo,
					drugsAlcoholInfo,
					drugsCannabisInfo,
					drugsOtherInfo,
					relationshipSingleInfo,
					relationshipTakenInfo,
					relationshipOpenInfo,
					relationshipOtherInfo,
					kidsNoInfo,
					kidsYesInfo));
		}
	}

	public void setDefaultAdmin() {
		if (userRepo.count() == 0) {
			User user = new User(adminEmail);
			user.setAdmin(true);
			String enc = passwordEncoder.encode(adminKey);
			user.setPassword(enc);
			userRepo.saveAndFlush(user);
		}
	}

	public void setDefaultGenders() {
		
		if (genderRepo.count() == 0) {
			Gender male = new Gender();
			male.setText("male");
			genderRepo.saveAndFlush(male);

			Gender female = new Gender();
			female.setText("female");
			genderRepo.saveAndFlush(female);

			Gender other = new Gender();
			other.setText("other");
			genderRepo.saveAndFlush(other);
		}

	}

	public void setDefaultIntentions() {
		if (userIntentionRepo.count() == 0) {
			UserIntention meet = new UserIntention();
			meet.setText("meet");
			userIntentionRepo.saveAndFlush(meet);

			UserIntention date = new UserIntention();
			date.setText("date");
			userIntentionRepo.saveAndFlush(date);

			UserIntention sex = new UserIntention();
			sex.setText("sex");
			userIntentionRepo.saveAndFlush(sex);
		}
	}

	public void removeInvalidUsers() throws AlovoaException {
		List<User> users = userRepo.findAll();

		UserDeleteParams userDeleteParam = UserDeleteParams.builder().conversationRepo(conversationRepo)
				.userBlockRepo(userBlockRepo).userHideRepo(userHideRepo).userLikeRepo(userLikeRepo)
				.userNotificationRepo(userNotificationRepo).userRepo(userRepo).userReportRepo(userReportRepo).build();

		for (User user : users) {

			if (!user.getEmail().contains("@")) {

				try {
					UserService.removeUserDataCascading(user, userDeleteParam);
					userRepo.delete(userRepo.findByEmail(user.getEmail()));
					userRepo.flush();

				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}
		}
	}
}
