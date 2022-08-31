package com.nonononoki.alovoa.html;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class UserProfileResource {

	@Autowired
	private UserRepository userRepo;

	@Autowired
	private AuthService authService;

	@Autowired
	private TextEncryptorConverter textEncryptor;


	@GetMapping("/profile/view/{idEncoded}")
	public String profileView(@PathVariable String idEncoded, Model model) throws NumberFormatException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
		Optional<Long> idOptional = UserDto.decodeId(idEncoded, textEncryptor);
		if (idOptional.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		Long id = idOptional.get();
		User userView = userRepo.findById(id).orElse(null);
		User user = authService.getCurrentUser(true);

		if (user.getId().equals(id)) {
			return "redirect:" + ProfileResource.URL;
		}

		if (userView != null) {
			if (userView.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()))) {
				throw new AlovoaException("blocked");
			}

			if (userView.isDisabled()) {
				throw new AlovoaException("disabled");
			}

			userView.setNumberProfileViews(userView.getNumberProfileViews() + 1);
			userView = userRepo.saveAndFlush(userView);

			UserDto userDto = UserDto.userToUserDto(userView, user, textEncryptor, UserDto.NO_AUDIO);
			UserDto currUserDto = UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA);

			model.addAttribute("user", userDto);
			model.addAttribute("currUser", currUserDto);

			model.addAttribute("compatible", Tools.usersCompatible(user, userView));

			return "user-profile";

		} else {
			throw new AlovoaException("user_not_found");
		}
	}

	@GetMapping("/profile/view/modal/{idEncoded}")
	public String warning(Model model, @PathVariable String idEncoded) throws AlovoaException, InvalidKeyException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, UnsupportedEncodingException {
		Optional<Long> idOptional = UserDto.decodeId(idEncoded, textEncryptor);
		if (idOptional.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}

		Long id = idOptional.get();
		User userView = userRepo.findById(id).orElse(null);
		User user = authService.getCurrentUser(true);

		if (user.getId().equals(id)) {
			return "";
		}

		if (userView != null) {
			if (userView.getBlockedUsers().stream().anyMatch(o -> o.getUserTo().getId().equals(user.getId()))) {
				throw new AlovoaException("blocked");
			}

			if (userView.isDisabled()) {
				throw new AlovoaException("disabled");
			}

			userView.setNumberProfileViews(userView.getNumberProfileViews() + 1);
			userView = userRepo.saveAndFlush(userView);

			UserDto userDto = UserDto.userToUserDto(userView, user, textEncryptor, UserDto.NO_AUDIO);
			UserDto currUserDto = UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA);

			model.addAttribute("user", userDto);
			model.addAttribute("currUser", currUserDto);
			model.addAttribute("compatible", Tools.usersCompatible(user, userView));

		} else {
			throw new AlovoaException("user_not_found");
		}

		return "fragments::user-profile-modal";
	}
}
