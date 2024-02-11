package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.entity.user.UserPrompt;
import com.nonononoki.alovoa.model.*;
import com.nonononoki.alovoa.service.UserPromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/prompt")
public class UserPromptController {

    @Autowired
    private UserPromptService promptService;

    @PostMapping("/delete/{promptId}")
    public void deletePrompt(@PathVariable long promptId) throws AlovoaException {
        promptService.deletePrompt(promptId);
    }

    @PostMapping("/add")
    public List<UserPrompt> addPrompt(@RequestBody UserPromptDto prompt) throws AlovoaException {
        return promptService.addPrompt(prompt);
    }

    @PostMapping("/update")
    public List<UserPrompt> updatePrompt(@RequestBody UserPromptDto prompt) throws AlovoaException {
        return promptService.updatePrompt(prompt);
    }
}
