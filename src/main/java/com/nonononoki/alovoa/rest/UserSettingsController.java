package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.service.UserSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user/settings")
public class UserSettingsController {

    @Autowired
    private UserSettingsService userSettingsService;

    @PostMapping(value="/emailLike/update/{value}")
    public void updateEmailLike(@PathVariable boolean value) throws AlovoaException {
        userSettingsService.updateEmailLike(value);
    }

    @PostMapping(value="/emailChat/update/{value}")
    public void updateEmailChat(@PathVariable boolean value) throws AlovoaException {
        userSettingsService.updateEmailChat(value);
    }

    @GetMapping(value = "/growth-privacy")
    public Map<String, Object> getGrowthPrivacySettings() throws AlovoaException {
        return userSettingsService.getGrowthPrivacySettings();
    }

    @PostMapping(value = "/shareGrowthProfile/update/{value}")
    public void updateShareGrowthProfile(@PathVariable boolean value) throws AlovoaException {
        userSettingsService.updateShareGrowthProfile(value);
    }

    @PostMapping(value = "/allowBehaviorSignals/update/{value}")
    public void updateAllowBehaviorSignals(@PathVariable boolean value) throws AlovoaException {
        userSettingsService.updateAllowBehaviorSignals(value);
    }

    @PostMapping(value = "/monthlyGrowthCheckins/update/{value}")
    public void updateMonthlyGrowthCheckins(@PathVariable boolean value) throws AlovoaException {
        userSettingsService.updateMonthlyGrowthCheckins(value);
    }

}
