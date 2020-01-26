package com.nonononoki.alovoa.html;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserRepository;

@Controller
public class UserProfileResource {
	
	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private TextEncryptorConverter textEncryptor;
	
	@GetMapping("/profile/view/{idEncoded}")
	public ModelAndView profileView(@PathVariable String idEncoded) throws NumberFormatException, Exception {
		long id = Long.parseLong(textEncryptor.decode(idEncoded));
		User user = userRepo.findById(id).orElse(null);
		if(user != null) {
			ModelAndView mav = new ModelAndView("userProfile");
			mav.addObject("user", user);
			return mav;
		} else {
			throw new Exception("");
		}		
	}
}
