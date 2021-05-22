package com.nonononoki.alovoa.html;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserBlock;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class BlockedUsersResource {
	
	@Autowired
	private AuthService authService;
	
	@Autowired
	private TextEncryptorConverter textEncryptor;
	
	@GetMapping("/blocked-users")
	public ModelAndView blockedUsers() throws NumberFormatException, Exception {
		
		User user = authService.getCurrentUser();
		ModelAndView mav = new ModelAndView("blocked-users");
		List<UserBlock> userBlocks = user.getBlockedUsers();
		List<User> blockedUsers = userBlocks.stream().map(b -> b.getUserTo()).collect(Collectors.toList());
		List<UserDto> users = new ArrayList<>();
		for(User u : blockedUsers) {
			users.add(UserDto.userToUserDto(u, user, textEncryptor, UserDto.PROFILE_PICTURE_ONLY));
		}
		mav.addObject("users", users);
		mav.addObject("user", UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA));
		return mav;	
	}
}
