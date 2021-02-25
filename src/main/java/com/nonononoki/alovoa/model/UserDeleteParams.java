package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.repo.UserBlockRepository;
import com.nonononoki.alovoa.repo.UserHideRepository;
import com.nonononoki.alovoa.repo.UserLikeRepository;
import com.nonononoki.alovoa.repo.UserNotificationRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.repo.UserRepository;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDeleteParams {
	
	private UserRepository userRepo;

	private UserLikeRepository userLikeRepo;

	private UserHideRepository userHideRepo;

	private UserBlockRepository userBlockRepo;

	private UserReportRepository userReportRepo;
	
	private UserNotificationRepository userNotificationRepo;

	private ConversationRepository conversationRepo;
}
