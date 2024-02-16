package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserSettings;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserSettingsService {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    public void updateEmailLike(boolean value) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        user.getUserSettings().setEmailLike(value);
        userRepository.saveAndFlush(user);
    }

    public void updateEmailChat(boolean value) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        user.getUserSettings().setEmailChat(value);
        userRepository.saveAndFlush(user);
    }
}
