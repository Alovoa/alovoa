package com.nonononoki.alovoa.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.entity.Captcha;
import com.nonononoki.alovoa.lib.OxCaptcha;
import com.nonononoki.alovoa.repo.CaptchaRepository;

@Service
public class CaptchaService {

	@Autowired
	private CaptchaRepository captchaRepo;

	@Autowired
	private HttpServletRequest request;

	@Value("${app.captcha.length}")
	private int captchaLength;
	
	private final int WIDTH = 120;
	private final int HEIGHT = 70;
	
	private final Color BG_COLOR = new Color(0, 0, 0, 0);
//	private final Color FG_COLOR = new Color(41, 182, 246);
//	private final Color FG_COLOR = new Color(236, 65, 122);
	private final Color FG_COLOR = new Color(130, 130, 130);

	public Captcha generate() throws Exception, IOException {
		OxCaptcha ox = generateCaptchaImage();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(ox.getImage(), "webp", baos);
		byte[] ba = baos.toByteArray();
		String encoded = Base64.getEncoder().encodeToString(ba);
		Captcha captcha = new Captcha();
		captcha.setDate(new Date());
		captcha.setImage(encoded);
		captcha.setText(ox.getText());
		captcha.setIp(request.getRemoteAddr());
		captcha = captchaRepo.saveAndFlush(captcha);
		return captcha;
	}
	
	private OxCaptcha generateCaptchaImage() {
		OxCaptcha c = new OxCaptcha(WIDTH, HEIGHT);
		c.foreground(FG_COLOR);
		c.background(BG_COLOR);
		c.text(captchaLength);
		
		c.distortion();
		c.noiseStraightLine();
		c.noiseStraightLine();
		c.noiseStraightLine();
		
		return c;	
	}

	public boolean isValid(long id, String text) {
		
		Captcha captcha = captchaRepo.findById(id).orElse(null);
		if (captcha == null) {
			return false;
		}		
		
		captchaRepo.delete(captcha);	
		
		if(!captcha.getIp().equals(request.getRemoteAddr())) {
			return false;
		}		
		if (!captcha.getText().toLowerCase().equals(text.toLowerCase())) {
			return false;
		}		
		return true;
	}
}
