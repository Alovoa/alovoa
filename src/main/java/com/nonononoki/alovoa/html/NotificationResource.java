package com.nonononoki.alovoa.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserNotification;
import com.nonononoki.alovoa.model.NotificationDto;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class NotificationResource {

	@Autowired
	private AuthService authService;

	@Autowired
	private TextEncryptorConverter textEncryptor;

	@Autowired
	private UserRepository userRepo;

	@GetMapping("/alerts")
	public ModelAndView notification() throws Exception {

		ModelAndView mav = new ModelAndView("notification");
		User user = authService.getCurrentUser();
		user.getDates().setNotificationCheckedDate(new Date());
		userRepo.saveAndFlush(user);
		List<UserNotification> nots = user.getNotifications();
		List<NotificationDto> notifications = new ArrayList<>();
		for (int i = 0; i < nots.size(); i++) {
			notifications.add(NotificationDto.notificationToNotificationDto(nots.get(i), user, textEncryptor));
		}
		Collections.sort(notifications, new Comparator<NotificationDto>() {
			@Override
			public int compare(NotificationDto a, NotificationDto b) {
				return b.getDate().compareTo(a.getDate());
			}
		});
		mav.addObject("notifications", notifications);
		mav.addObject("user", UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA));
		return mav;
	}
}
