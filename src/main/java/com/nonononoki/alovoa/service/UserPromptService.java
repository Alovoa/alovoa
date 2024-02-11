package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserPrompt;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserPromptDto;
import com.nonononoki.alovoa.repo.UserPromptRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserPromptService {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AuthService authService;

    @Value("${app.prompt.max}")
    private int promptMax;

    @Value("${app.prompt.length.max}")
    private int promptLengthMax;

    public void deletePrompt(long promptId) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        user.getPrompts().removeIf(e -> e.getPromptId() == promptId);
        userRepo.saveAndFlush(user);
    }

    public List<UserPrompt> addPrompt(UserPromptDto promptDto) throws AlovoaException {
        validatePromptTextLength(promptDto);
        User user = authService.getCurrentUser(true);
        if (user.getPrompts().size() >= promptMax) {
            throw new AlovoaException("Max number of prompts exceeded");
        } else if (user.getPrompts().stream().anyMatch(o -> Objects.equals(o.getPromptId(),
                promptDto.getPromptId()))) {
            throw new AlovoaException("Prompt with same promptId already exists");
        } else {
            UserPrompt prompt = new UserPrompt();
            prompt.setUser(user);
            prompt.setPromptId(promptDto.getPromptId());
            prompt.setText(promptDto.getText());
            user.getPrompts().add(prompt);
            return userRepo.saveAndFlush(user).getPrompts();
        }
    }

    public List<UserPrompt> updatePrompt(UserPromptDto promptDto) throws AlovoaException {
        User user = authService.getCurrentUser(true);
        validatePromptTextLength(promptDto);
        Optional<UserPrompt> promptOpt = user.getPrompts().stream().filter(o ->
                Objects.equals(o.getPromptId(), promptDto.getPromptId())).findFirst();
        if (promptOpt.isPresent()) {
            UserPrompt prompt = promptOpt.get();
            prompt.setText(promptDto.getText());
            user.getPrompts().add(prompt);
            return userRepo.saveAndFlush(user).getPrompts();
        } else {
            throw new AlovoaException("Prompt with promptId does not exist");
        }
    }

    private void validatePromptTextLength(UserPromptDto promptDto) throws AlovoaException {
        if (promptDto.getText().length() > promptLengthMax) {
            throw new AlovoaException("Max prompt text length exceeded");
        }
    }
}
