package com.nonononoki.alovoa.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserWebPush;
import com.nonononoki.alovoa.html.NotificationResource;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.WebPushMessage;
import com.nonononoki.alovoa.repo.UserRepository;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushAsyncService;

@Service
public class NotificationService {

	@Value("${app.vapid.public}")
	private String vapidPublicKey;

	@Value("${app.vapid.private}")
	private String vapidPrivateKey;

	@Value("${app.vapid.max}")
	private int vapidMax;

	@Value("${app.domain}")
	private String appDomain;

	@Autowired
	private AuthService authService;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private UserRepository userRepo;

	private PushAsyncService pushService;

	public PushAsyncService pushService()
			throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		if (pushService == null) {
			pushService = new PushAsyncService();
			pushService.setPrivateKey(vapidPrivateKey);
			pushService.setPublicKey(vapidPublicKey);
		}
		return pushService;
	}

	public void subscribe(UserWebPush webPush) throws AlovoaException {
		User user = authService.getCurrentUser(true);
		webPush.setUser(user);
		if (webPush.getDate() == null) {
			webPush.setDate(new Date());
		}
		user.getWebPush().add(webPush);
		user = userRepo.saveAndFlush(user);

		if (user.getWebPush().size() > vapidMax) {
			UserWebPush wp = Collections.min(user.getWebPush(), Comparator.comparing(UserWebPush::getDate));
			user.getWebPush().remove(wp);
			userRepo.saveAndFlush(user);
		}
	}

	public void newLike(User user) throws GeneralSecurityException, IOException, JoseException {
		user.getDates().setNotificationDate(new Date());
		user = userRepo.saveAndFlush(user);

		Locale locale = Tools.getUserLocale(user);
		String title = messageSource.getMessage("backend.webpush.like.message", null, locale);
		String msg = messageSource.getMessage("backend.webpush.like.subject", null, locale);

		WebPushMessage message = new WebPushMessage();
		message.setClickTarget(appDomain + NotificationResource.URL);
		message.setTitle(title);
		message.setMessage(msg);
		send(user, message);
	}

	public void newMatch(User user) throws GeneralSecurityException, IOException, JoseException {
		user.getDates().setMessageDate(new Date());
		user = userRepo.saveAndFlush(user);

		Locale locale = Tools.getUserLocale(user);
		String title = messageSource.getMessage("backend.webpush.match.message", null, locale);
		String msg = messageSource.getMessage("backend.webpush.match.subject", null, locale);

		WebPushMessage message = new WebPushMessage();
		message.setClickTarget(appDomain + NotificationResource.URL);
		message.setTitle(title);
		message.setMessage(msg);
		send(user, message);
	}

	public void newMessage(User user) throws GeneralSecurityException, IOException, JoseException {
		user.getDates().setMessageDate(new Date());
		user = userRepo.saveAndFlush(user);

		Locale locale = Tools.getUserLocale(user);
		String title = messageSource.getMessage("backend.webpush.message.message", null, locale);
		String msg = messageSource.getMessage("backend.webpush.message.subject", null, locale);

		WebPushMessage message = new WebPushMessage();
		message.setClickTarget(appDomain + NotificationResource.URL);
		message.setTitle(title);
		message.setMessage(msg);
		send(user, message);
	}

	private void send(User user, WebPushMessage message) throws GeneralSecurityException, IOException, JoseException {
		List<UserWebPush> pushes = user.getWebPush();
		for (UserWebPush uwp : pushes) {
			Notification notification = new Notification(uwp.getEndPoint(), uwp.getPublicKey(), uwp.getAuth(),
					objectMapper.writeValueAsBytes(message));
			pushService().send(notification);
		}
	}
}
