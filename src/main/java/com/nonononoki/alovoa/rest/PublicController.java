package com.nonononoki.alovoa.rest;

import lombok.AllArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nonononoki.alovoa.service.PublicService;

@RestController
@RequestMapping("/")
@AllArgsConstructor
public class PublicController {

	private PublicService publicService;

	@GetMapping(value="/text/{value:.+}", produces = "text/plain; charset=UTF-8")
	public String text(Model model, @PathVariable String value) {
		return publicService.text(value);
	}
}
