package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.service.UserSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/settings/update")
public class UserSettingsController {

    @Autowired
    private UserSettingsService userSettingsService;

    @PostMapping(value="/emailLike/{value}")
    public void updateEmailLike(@PathVariable boolean value) throws AlovoaException {
        userSettingsService.updateEmailLike(value);
    }

    @PostMapping(value="/emailMatch/{value}")
    public void updateEmailMatch(@PathVariable boolean value) throws AlovoaException {
        userSettingsService.updateEmailMatch(value);
    }

    @PostMapping(value="/emailChat/{value}")
    public void updateEmailChat(@PathVariable boolean value) throws AlovoaException {
        userSettingsService.updateEmailChat(value);
    }

}
