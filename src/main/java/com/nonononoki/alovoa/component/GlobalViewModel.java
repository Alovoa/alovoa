package com.nonononoki.alovoa.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewModel {
    @Value("${app.url.front-end}")
    private String frontEndUrl;

    @ModelAttribute("frontEndUrl")
    public String frontEndUrl() {
        return frontEndUrl;
    }
}