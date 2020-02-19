package com.nonononoki.alovoa.rest;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.nonononoki.alovoa.model.DonationDto;
import com.nonononoki.alovoa.model.DonationKofi;
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
	
	@PostMapping(value="/received/kofi", consumes = "application/x-www-form-urlencoded")
    public void receivedKofi(@RequestBody DonationKofi d, HttpServletRequest request) throws Exception{
		//TODO Check if RequestBody works
		donateService.donationReceivedKofi(d, request);
	}
}
