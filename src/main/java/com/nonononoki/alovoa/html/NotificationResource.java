package com.nonononoki.alovoa.html;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserNotification;
import com.nonononoki.alovoa.model.AlovoaException;
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

	public static final String URL = "/alerts";

	@GetMapping(URL)
	public ModelAndView notification() throws AlovoaException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			UnsupportedEncodingException {

		ModelAndView mav = new ModelAndView("notification");
		User user = authService.getCurrentUser();
		user.getDates().setNotificationCheckedDate(new Date());
		userRepo.saveAndFlush(user);
		List<UserNotification> nots = user.getNotifications();
		List<NotificationDto> notifications = new ArrayList<>();
		for (int i = 0; i < nots.size(); i++) {
			UserNotification n = nots.get(i);

			boolean blockedMe = user.getBlockedByUsers().stream()
					.anyMatch(b -> b.getUserFrom().getId().equals(n.getUserFrom().getId()));
			boolean blockedYou = user.getBlockedUsers().stream()
					.anyMatch(b -> b.getUserTo().getId().equals(n.getUserFrom().getId()));

			if (!blockedMe && !blockedYou) {
				NotificationDto dto = NotificationDto.notificationToNotificationDto(n, user, textEncryptor);
				notifications.add(dto);
			}
		}

		notifications.sort((NotificationDto a, NotificationDto b) -> b.getDate().compareTo(a.getDate()));

		mav.addObject("notifications", notifications);
		mav.addObject("user", UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA));
		return mav;
	}
}
