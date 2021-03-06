package com.nonononoki.alovoa.html;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.PublicService;

@Controller
public class TosResource {

	@Autowired
	private PublicService publicService;

	@Autowired
	private AuthService authService;

	@Autowired
	private TextEncryptorConverter textEncryptor;

	@Value("${app.company.name}")
	private String companyName;

	@Value("${app.tos.update-date}")
	private String tosUpdateDate;

	@Value("${app.age.min}")
	private String minAge;

	private static final String COMPANY_NAME = "COMPANY_NAME";
	private static final String TOS_UPDATE_DATE = "TOS_UPDATE_DATE";
	private static final String MIN_AGE = "MIN_AGE";

	@GetMapping("/tos")
	public ModelAndView tosConditions() throws AlovoaException, InvalidKeyException, IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
			UnsupportedEncodingException {
		ModelAndView mav = new ModelAndView("tos");
		String content = publicService.text("backend.tos");
		content = content.replace(COMPANY_NAME, companyName);
		content = content.replace(TOS_UPDATE_DATE, tosUpdateDate);
		content = content.replace(MIN_AGE, minAge);
		mav.addObject("content", content);
		User user = authService.getCurrentUser();
		mav.addObject("user", UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA));
		return mav;
	}
}
