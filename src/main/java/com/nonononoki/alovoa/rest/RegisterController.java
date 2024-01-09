package com.nonononoki.alovoa.rest;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import jakarta.mail.MessagingException;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.RegisterDto;
import com.nonononoki.alovoa.service.RegisterService;

@RestController
@RequestMapping("/")
public class RegisterController {

	@Autowired
	private RegisterService registerService;

	@PostMapping(value = "/register", consumes = "application/json")
	public void register(@RequestBody RegisterDto dto)
			throws NoSuchAlgorithmException, AlovoaException, MessagingException, IOException {
		registerService.register(dto);
	}

	@PostMapping(value = "/register-oauth", consumes = "application/json")
	public String registerOauth(@RequestBody RegisterDto dto) throws MessagingException, IOException, AlovoaException,
			NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
		registerService.registerOauth(dto);
		return "profile";
	}

	@GetMapping("/register/confirm/{tokenString}")
	public void registerConfirm(@PathVariable String tokenString, HttpServletResponse response) throws IOException {
		try {
			registerService.registerConfirm(tokenString);
			response.sendRedirect("/?registration-confirm-success");
		} catch (Exception e) {
			response.sendRedirect("/?registration-confirm-failed");
		}
	}
}
