package com.nonononoki.alovoa.component;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.CustomOAuth2User;
import com.nonononoki.alovoa.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        String email = oauthUser.getAttribute("email");
        User user = userRepo.findByEmail(email);
        if(user == null) {
            return oauthUser;
        }
        return new CustomOAuth2User(oauthUser, user.getUuid());
    }
}
