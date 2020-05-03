package com.nonononoki.alovoa.rest;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.model.UserDeleteAccountDto;
import com.nonononoki.alovoa.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

	@Autowired
	private UserService userService;

	// GDPR
	@PostMapping(value = "/delete-account", consumes = "text/plain") 
	public void deleteAccount(@RequestBody String password) throws MessagingException {
		userService.deleteAccountRequest(password);
	}
	
	@PostMapping(value = "/delete-account-confirm", consumes = "application/json")
	public void deleteAccount(@RequestBody UserDeleteAccountDto dto) throws Exception {
		userService.deleteAccountConfirm(dto);
	}

	@PostMapping(value = "/userdata", consumes = "text/plain")
	public void getUserdata(@RequestBody String password) throws Exception {
		userService.getUserdata(password);
	}

	@PostMapping(value = "/update/profile-picture", consumes = "text/plain")
	public void updateProfilePicture(@RequestBody String imageB64) throws Exception {
		userService.updateProfilePicture(imageB64);
	}

	@PostMapping(value = "/update/description", consumes = "text/plain")
	public void updateDescription(@RequestBody String description) throws Exception {
		userService.updateDescription(description);
	}

	@PostMapping("/update/intention/{intention}")
	public void updateIntention(@PathVariable long intention) {
		userService.updateIntention(intention);
	}

	@PostMapping("/update/min-age/{minAge}")
	public void updateMinAge(@PathVariable int minAge) throws Exception {
		userService.updateMinAge(minAge);
	}

	@PostMapping("/update/max-age/{maxAge}")
	public void updateMaxAge(@PathVariable int maxAge) throws Exception {
		userService.updateMaxAge(maxAge);
	}

	@PostMapping("/update/preferedGender/{genderId}/{activated}")
	public void updatePreferedGenders(@PathVariable int genderId, @PathVariable String activated) {
		userService.updatePreferedGender(genderId, Tools.binaryStringToBoolean(activated));
	}

	@PostMapping("/update/interest/{interest}/{activated}")
	public void updateInterest(@PathVariable int interest, @PathVariable String activated) {
		userService.updateInterest(interest, Tools.binaryStringToBoolean(activated));
	}
	
	@PostMapping("/update/theme/{themeId}")
	public void updateTheme(@PathVariable int themeId) {
		userService.updateTheme(themeId);
	}

	@PostMapping(value = "/like/{idEnc}")
	public void likeUser(@PathVariable String idEnc) throws Exception {
		userService.likeUser(idEnc);
	}

	@PostMapping(value = "/hide/{idEnc}")
	public void hideUser(@PathVariable String idEnc) throws Exception {
		userService.hideUser(idEnc);
	}

	@PostMapping(value = "/block/{idEnc}")
	public void blockUser(@PathVariable String idEnc) throws Exception {
		userService.blockUser(idEnc);
	}
	
	@PostMapping(value = "/unblock/{idEnc}")
	public void unblockUser(@PathVariable String idEnc) throws Exception {
		userService.unblockUser(idEnc);
	}

	@PostMapping(value = "/report/{idEnc}/{captchaId}/{captchaText}")
	public void reportUser(@PathVariable String idEnc, @PathVariable long captchaId, @PathVariable String captchaText) throws Exception {
		userService.reportUser(idEnc, captchaId, captchaText);
	}
	
	@GetMapping(value = "/status/new-alert")
	public boolean newAlert() throws Exception {
		return userService.hasNewAlert();
	}
	
	@GetMapping(value = "/status/new-message")
	public boolean newMessage() throws Exception {
		return userService.hasNewMessage();
	}
	
}
