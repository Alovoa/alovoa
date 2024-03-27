package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserImage;
import com.nonononoki.alovoa.rest.MediaController;
import com.nonononoki.alovoa.service.UserService;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserImageDto {

    private Long id;
    private String content;

    public static List<UserImageDto> buildFromUserImages(User user, UserService userService) {
        List<UserImageDto> list = new ArrayList<>();
        for (int i = 0; i < user.getImages().size(); i++) {
            list.add(UserImageDto.builder().content(userService.getDomain() + MediaController.URL_REQUEST_MAPPING +
                    MediaController.URL_IMAGE + Tools.getImageUUID(user.getImages().get(i), userService))
                    .id(user.getImages().get(i).getId()).build());
        }
        return list;
    }
}
