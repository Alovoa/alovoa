package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.repo.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserDeleteParams {

    private UserRepository userRepo;

    private UserLikeRepository userLikeRepo;

    private UserHideRepository userHideRepo;

    private UserBlockRepository userBlockRepo;

    private UserReportRepository userReportRepo;

    private UserNotificationRepository userNotificationRepo;

    private ConversationRepository conversationRepo;

    private UserVerificationPictureRepository userVerificationPictureRepo;
}
