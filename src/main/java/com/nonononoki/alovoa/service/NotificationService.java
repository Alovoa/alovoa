package com.nonononoki.alovoa.service;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserWebPush;
import com.nonononoki.alovoa.model.WebPushMessage;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.repo.UserWebPushRepository;

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
	private UserWebPushRepository userWebPushRepo;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private HttpServletRequest request;

	private static PushService pushService;

	public PushService pushService() throws Exception {
		if (pushService == null) {
			pushService = new PushService();
			pushService.setPrivateKey(vapidPrivateKey);
			pushService.setPublicKey(vapidPublicKey);
		}
		return pushService;
	}

	@Autowired
	private UserRepository userRepo;

	public void subscribe(UserWebPush webPush) {
		webPush.setUser(authService.getCurrentUser());
		String ip = request.getRemoteAddr();
		webPush.setIp(ip);	
		userWebPushRepo.deleteAll(userWebPushRepo.findAllByIp(ip));
		userWebPushRepo.save(webPush);
	}

	public void newLike(User user) throws Exception {
		user.getDates().setNotificationDate(new Date());
		user = userRepo.saveAndFlush(user);

		// TODO
		WebPushMessage message = new WebPushMessage();
		message.setClickTarget(appDomain + "/alerts");
		message.setMessage("Click here to find out more");
		message.setTitle("New like!");
		send(user, message);
	}

	public void newMatch(User user) throws Exception {
		user.getDates().setMessageDate(new Date());
		user = userRepo.saveAndFlush(user);

		// TODO
		WebPushMessage message = new WebPushMessage();
		message.setClickTarget(appDomain + "/chats");
		message.setMessage("Click here to find out more");
		message.setTitle("New match!");
		send(user, message);
	}

	public void newMessage(User user) throws Exception {
		user.getDates().setMessageDate(new Date());
		user = userRepo.saveAndFlush(user);

		// TODO
		WebPushMessage message = new WebPushMessage();
		message.setClickTarget(appDomain + "/chats");
		message.setMessage("Click here to find out more");
		message.setTitle("New message!");
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
