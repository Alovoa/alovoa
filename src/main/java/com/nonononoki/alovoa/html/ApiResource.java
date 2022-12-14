package com.nonononoki.alovoa.html;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nonononoki.alovoa.model.AlovoaException;

@RestController
@RequestMapping("/api/v1/resource")
public class ApiResource {

	@Autowired
	private DonateResource donateResource;

	@Autowired
	private MessageResource messageResource;

	@Autowired
	private NotificationResource notificationResource;

	@Autowired
	private ProfileResource profileResource;

	@Autowired
	private SearchResource searchResource;

	@Autowired
	private UserProfileResource userProfileResource;
	
	@Autowired
	private ProfileOnboardingResource profileOnboardingResource;

	@Autowired
	private LikedUsersResource likedUsersResource;

	@Autowired
	private DislikedUsersResource dislikedUsersResource;

	@Autowired
	private BlockedUsersResource blockedUsersResource;

	@GetMapping("/donate")
	public Map<String, Object> resourceDonate() throws JsonProcessingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		return donateResource.donate().getModel();
	}

	@GetMapping("/chats")
	public Map<String, Object> resourceChats()
			throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		return messageResource.chats().getModel();
	}

	@GetMapping("/chats/{id}")
	public Map<String, Object> resourceChatsDetail(@PathVariable long id) throws JsonProcessingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		return messageResource.chatsDetail(id).getModel();
	}

	@GetMapping("/alerts")
	public Map<String, Object> resourceNotification() throws JsonProcessingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		return notificationResource.notification().getModel();
	}

	@GetMapping("/profile")
	public Map<String, Object> resourceProfile() throws JsonProcessingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		return profileResource.profile().getModel();
	}
	
	@GetMapping("/profile/view/{idEncoded}")
	public Map<String, Object> resourceProfileView(@PathVariable String idEncoded) throws JsonProcessingException,
			InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		return userProfileResource.profileView(idEncoded).getModel();
	}

	@GetMapping("/search")
	public Map<String, Object> resourceSearch() throws JsonProcessingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		return searchResource.search().getModel();
	}
	
	@GetMapping("/user/onboarding")
	public Map<String, Object> userOnboarding() throws JsonProcessingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		return profileOnboardingResource.onboarding().getModel();
	}

	@GetMapping("/blocked-users")
	public Map<String, Object> blockedUsers() throws JsonProcessingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		return blockedUsersResource.blockedUsers().getModel();
	}

	@GetMapping("/liked-users")
	public Map<String, Object> likedUsers() throws JsonProcessingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		return likedUsersResource.likedUsers().getModel();
	}

	@GetMapping("/disliked-users")
	public Map<String, Object> dislikedUsers() throws JsonProcessingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		return dislikedUsersResource.dislikedUsers().getModel();
	}
}
