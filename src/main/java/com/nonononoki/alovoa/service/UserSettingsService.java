package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserSettings;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserSettingsService {

    private AuthService authService;

    private UserRepository userRepository;

    public void updateEmailLike(boolean value) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        UserSettings settings = user.getUserSettings();
        user.setUserSettings(settings);
        settings.setEmailLike(value);
        userRepository.saveAndFlush(user);
    }

    public void updateEmailChat(boolean value) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        UserSettings settings = user.getUserSettings();
        user.setUserSettings(settings);
        settings.setEmailChat(value);
        userRepository.saveAndFlush(user);
    }
}
