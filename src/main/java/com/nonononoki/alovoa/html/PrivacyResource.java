package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.PublicService;

@Controller
public class PrivacyResource {
	
	@Autowired
	private PublicService publicService;
	
	@Autowired
	private AuthService authService;
	
	@Autowired
	private TextEncryptorConverter textEncryptor;
	
	@Value("${app.company.name}")
	private String companyName;
	
	@Value("${app.privacy.update-date}")
	private String privacyUpdateDate;
	
	private static final String COMPANY_NAME = "COMPANY_NAME";
	private static final String PRIVACY_UPDATE_DATE = "PRIVACY_UPDATE_DATE";
	
	@GetMapping("/privacy")
	public ModelAndView privacy() throws Exception {
		ModelAndView mav = new ModelAndView("privacy");
		String content = publicService.text("backend.privacy");
		content = content.replace(COMPANY_NAME, companyName);
		content = content.replace(PRIVACY_UPDATE_DATE, privacyUpdateDate);
		mav.addObject("content", content);
		User user = authService.getCurrentUser();
		mav.addObject("user", UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA));
		return mav;
	}
}
