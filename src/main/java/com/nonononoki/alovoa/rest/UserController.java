package com.nonononoki.alovoa.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserService userService;
	
	@GetMapping("/delete/request")
    public void deleteRequest(String password){
        //TODO
		
    }
	
	@GetMapping("/userdata")
    public void getUserdata(String password){
        //TODO
		
    }
	
	@PostMapping(value="/update/profile-picture", consumes = "text/plain")
    public void updateProfilePicture(@RequestBody String imageB64) throws Exception{
		userService.updateProfilePicture(imageB64);
    }
	
	@PostMapping(value="/update/description", consumes = "text/plain")
    public void updateDescription(@RequestBody String description) throws Exception{
		userService.updateDescription(description);
    }
	
	@PostMapping("/update/intention/{intention}")
    public void updateIntention(@PathVariable long intention){
		userService.updateIntention(intention);
    }
	
	@PostMapping("/update/min-age/{minAge}")
    public void updateMinAge(@PathVariable int minAge) throws Exception{
		userService.updateMinAge(minAge);	
    }
	
	@PostMapping("/update/max-age/{maxAge}")
    public void updateMaxAge(@PathVariable int maxAge) throws Exception{
		userService.updateMaxAge(maxAge);
    }
	
	@PostMapping("/update/preferedGender/{genderId}/{activated}")
    public void updatePreferedGenders(@PathVariable int genderId, @PathVariable String activated){
		userService.updatePreferedGender(genderId, Tools.binaryStringToBoolean(activated));
    }
	
	@PostMapping("/update/interest/{interest}/{activated}")
    public void updateInterest(@PathVariable int interest, @PathVariable String activated){
		userService.updateInterest(interest, Tools.binaryStringToBoolean(activated));
    }
}
