package com.nonononoki.alovoa.service;

import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserWebPush;
import com.nonononoki.alovoa.model.WebPushMessage;
import com.nonononoki.alovoa.repo.UserRepository;

import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;

@Service
public class NotificationService {

	@Value("${app.vapid.public}")
	private String vapidPublicKey;

	@Value("${app.vapid.private}")
	private String vapidPrivateKey;
	
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

	private static PushService pushService;

	public PushService pushService() throws Exception {
		if (pushService == null) {
			pushService = new PushService();
			pushService.setPrivateKey(vapidPrivateKey);
			pushService.setPublicKey(vapidPublicKey);
		}
		return pushService;
	}

	public void subscribe(UserWebPush webPush) throws Exception {
		User user = authService.getCurrentUser();
		webPush.setUser(user);
		webPush.setDate(new Date());
		user.getWebPush().add(webPush);
		userRepo.saveAndFlush(user);
	}

	public void newLike(User user) throws Exception {
		user.getDates().setNotificationDate(new Date());
		user = userRepo.saveAndFlush(user);

		Locale locale = Tools.getUserLocale(user);
		String title = messageSource.getMessage("backend.webpush.like.message", null, locale);
		String msg = messageSource.getMessage("backend.webpush.like.subject", null, locale);

		WebPushMessage message = new WebPushMessage();
		message.setClickTarget(appDomain + "/alerts");
		message.setTitle(title);
		message.setMessage(msg);
		send(user, message);
	}

	public void newMatch(User user) throws Exception {
		user.getDates().setMessageDate(new Date());
		user = userRepo.save(user);

		Locale locale = Tools.getUserLocale(user);
		String title = messageSource.getMessage("backend.webpush.match.message", null, locale);
		String msg = messageSource.getMessage("backend.webpush.match.subject", null, locale);

		WebPushMessage message = new WebPushMessage();
		message.setClickTarget(appDomain + "/alerts");
		message.setTitle(title);
		message.setMessage(msg);
		send(user, message);
	}

	public void newMessage(User user) throws Exception {
		user.getDates().setMessageDate(new Date());
		user = userRepo.saveAndFlush(user);

		Locale locale = Tools.getUserLocale(user);
		String title = messageSource.getMessage("backend.webpush.message.message", null, locale);
		String msg = messageSource.getMessage("backend.webpush.message.subject", null, locale);

		WebPushMessage message = new WebPushMessage();
		message.setClickTarget(appDomain + "/alerts");
		message.setTitle(title);
		message.setMessage(msg);
		send(user, message);
	}

	private void send(User user, WebPushMessage message) throws Exception {
		List<UserWebPush> pushes = user.getWebPush();
		for (UserWebPush uwp : pushes) {
			Notification notification = new Notification(uwp.getEndPoint(), uwp.getPublicKey(), uwp.getAuth(),
					objectMapper.writeValueAsBytes(message));
			pushService().send(notification);
		}
	}
}
