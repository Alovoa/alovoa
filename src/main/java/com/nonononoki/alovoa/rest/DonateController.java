package com.nonononoki.alovoa.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nonononoki.alovoa.model.DonationDto;
import com.nonononoki.alovoa.service.DonateService;

@Controller
@RequestMapping("/donate")
public class DonateController {
	
	@Autowired
	private DonateService donateService; 	

	@GetMapping("/search/{filter}")
    public String filterRecent(Model model, @PathVariable int filter) throws Exception{
		List<DonationDto> donations = donateService.filter(filter);
		model.addAttribute("donations", donations);
		return "fragments :: donate-filter";
    }
}
