package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user-settings")
public class UserSettingsController {

    @Autowired
    private AuthService authService;

    @PostMapping(value="/update/emailLike/{value}")
    public void updateEmailLike(@PathVariable boolean value) throws AlovoaException {
       authService.getCurrentUser().getUserSettings().setEmailLike(value);
    }

    @PostMapping(value="/update/emailMatch/{value}")
    public void updateEmailMatch(@PathVariable boolean value) throws AlovoaException {
        authService.getCurrentUser().getUserSettings().setEmailMatch(value);
    }

    @PostMapping(value="/update/emailChat/{value}")
    public void updateEmailChat(@PathVariable boolean value) throws AlovoaException {
        authService.getCurrentUser().getUserSettings().setEmailChat(value);
    }

}
