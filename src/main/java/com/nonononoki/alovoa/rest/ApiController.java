package com.nonononoki.alovoa.rest;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.html.DonateResource;
import com.nonononoki.alovoa.html.MessageResource;
import com.nonononoki.alovoa.html.NotificationResource;
import com.nonononoki.alovoa.html.ProfileResource;
import com.nonononoki.alovoa.html.SearchResource;
import com.nonononoki.alovoa.html.UserProfileResource;
import com.nonononoki.alovoa.model.AlovoaException;

@RestController
public class ApiController {

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
	private ObjectMapper objectMapper;

	@GetMapping("/api/v1/resource/donate")
	public String resourceDonate() throws JsonProcessingException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			UnsupportedEncodingException, AlovoaException {
		return objectMapper.writeValueAsString(donateResource.donate().getModel());
	}

	@GetMapping("/api/v1/resource/chats")
	public String resourceChats() throws JsonProcessingException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			UnsupportedEncodingException, AlovoaException {
		return objectMapper.writeValueAsString(messageResource.chats().getModel());
	}

	@GetMapping("/api/v1/resource/chats/{id}")
	public String resourceChatsDetail(@PathVariable long id) throws JsonProcessingException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		return objectMapper.writeValueAsString(messageResource.chatsDetail(id).getModel());
	}

	@GetMapping("/api/v1/resource/alerts")
	public String resourceNotification() throws JsonProcessingException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			UnsupportedEncodingException, AlovoaException {
		return objectMapper.writeValueAsString(notificationResource.notification().getModel());
	}

	@GetMapping("/api/v1/resource/profile")
	public String resourceProfile() throws JsonProcessingException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			UnsupportedEncodingException, AlovoaException {
		return objectMapper.writeValueAsString(profileResource.profile().getModel());
	}

	@GetMapping("/api/v1/resource/search")
	public String resourceSearch() throws JsonProcessingException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			UnsupportedEncodingException, AlovoaException {
		return objectMapper.writeValueAsString(searchResource.search().getModel());
	}
	
	@GetMapping("/api/v1/resource//profile/view/{idEncoded}")
	public String resourceProfileView(@PathVariable String idEncoded) throws JsonProcessingException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			UnsupportedEncodingException, AlovoaException {
		return objectMapper.writeValueAsString(userProfileResource.profileView(idEncoded).getModel());
	}

}
