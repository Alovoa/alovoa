package com.nonononoki.alovoa.model;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.rest.MediaController;
import com.nonononoki.alovoa.service.UserService;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class UserImageDto {

    private String content;

    public static List<UserImageDto> buildFromUserImages(User user, String idEnc, UserService userService) {
        List<UserImageDto> list = new ArrayList<>();
        for (int i = 0; i < user.getImages().size(); i++) {
            list.add(UserImageDto.builder().content(userService.getDomain() + MediaController.URL_REQUEST_MAPPING +
                    MediaController.URL_IMAGE + idEnc + "/" + i).build());
        }
        return list;
    }
}
