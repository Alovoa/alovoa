package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserSettings;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserSettingsService {

    @Autowired
    private AuthService authService;

    @Autowired
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

    public void updateShareGrowthProfile(boolean value) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        UserSettings settings = user.getUserSettings();
        user.setUserSettings(settings);
        settings.setShareGrowthProfile(value);
        userRepository.saveAndFlush(user);
    }

    public void updateAllowBehaviorSignals(boolean value) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        UserSettings settings = user.getUserSettings();
        user.setUserSettings(settings);
        settings.setAllowBehaviorSignals(value);
        userRepository.saveAndFlush(user);
    }

    public void updateMonthlyGrowthCheckins(boolean value) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        UserSettings settings = user.getUserSettings();
        user.setUserSettings(settings);
        settings.setMonthlyGrowthCheckins(value);
        userRepository.saveAndFlush(user);
    }

    public Map<String, Object> getGrowthPrivacySettings() throws AlovoaException {
        User user = authService.getCurrentUser(true);
        UserSettings settings = user.getUserSettings();
        return Map.of(
                "shareGrowthProfile", settings.isShareGrowthProfile(),
                "allowBehaviorSignals", settings.isAllowBehaviorSignals(),
                "monthlyGrowthCheckins", settings.isMonthlyGrowthCheckins()
        );
    }
}
