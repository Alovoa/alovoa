package com.nonononoki.alovoa.html;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.Contact;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserReport;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.ContactRepository;
import com.nonononoki.alovoa.repo.UserReportRepository;
import com.nonononoki.alovoa.service.AuthService;

@Controller
public class AdminResource {

	@Autowired
	private UserReportRepository userReportRepo;
	
	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private TextEncryptorConverter textEncryptor;
	
	@Autowired
	private AuthService authService;

	@GetMapping("/admin")
	public ModelAndView admin() throws Exception {

		ModelAndView mav = new ModelAndView("admin");

		List<UserReport> reports = userReportRepo.findTop20ByOrderByDateAsc();
		
		for(UserReport r : reports) {
			r.setUserToIdEnc(UserDto.encodeId(r.getUserTo().getId(), textEncryptor));
		}

		List<Contact> contacts = contactRepository.findTop20ByHiddenFalse();

		mav.addObject("reports", reports);
		mav.addObject("contacts", contacts);
		User user = authService.getCurrentUser();
		mav.addObject("user", UserDto.userToUserDto(user, user, textEncryptor, UserDto.NO_MEDIA));
		
		return mav;
	}
}
