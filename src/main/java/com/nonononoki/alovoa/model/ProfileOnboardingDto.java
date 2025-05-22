package com.nonononoki.alovoa.model;

import java.util.List;
import java.util.Set;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class ProfileOnboardingDto {

    private Long intention;

    private List<Long> preferredGenders;

    private String profilePictureMime;

    private String description;

    private Set<String> interests;

    private boolean notificationLike;

    private boolean notificationChat;

}
