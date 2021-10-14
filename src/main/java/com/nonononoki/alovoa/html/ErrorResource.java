package com.nonononoki.alovoa.html;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nonononoki.alovoa.model.AlovoaException;

@Controller
public class ErrorResource {
	
	@GetMapping("/error")
	public ModelAndView error() throws AlovoaException, JsonProcessingException {
		return new ModelAndView("error");
	}
}
