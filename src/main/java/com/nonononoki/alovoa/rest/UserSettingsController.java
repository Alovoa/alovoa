package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.service.UserSettingsService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user/settings")
@AllArgsConstructor
public class UserSettingsController {

    private UserSettingsService userSettingsService;

    @PostMapping(value="/emailLike/update/{value}")
    public void updateEmailLike(@PathVariable boolean value) throws AlovoaException {
        userSettingsService.updateEmailLike(value);
    }

    @PostMapping(value="/emailChat/update/{value}")
    public void updateEmailChat(@PathVariable boolean value) throws AlovoaException {
        userSettingsService.updateEmailChat(value);
    }

}
