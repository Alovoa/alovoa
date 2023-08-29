package com.nonononoki.alovoa.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletRequest;

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

	@Value("${app.text.salt}")
	private String salt;

	private static final int WIDTH = 120;
	private static final int HEIGHT = 70;

	private static final Color BG_COLOR = new Color(0, 0, 0, 0);
	private static final Color FG_COLOR = new Color(118, 118, 118);

	public Captcha generate() throws NoSuchAlgorithmException, IOException {

		String ipHash = getIpHash(request.getRemoteAddr());
		Captcha oldCaptcha = captchaRepo.findByHashCode(ipHash);
		if (oldCaptcha != null) {
			captchaRepo.delete(oldCaptcha);
			captchaRepo.flush();
		}

		OxCaptcha ox = generateCaptchaImage();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(ox.getImage(), "webp", baos);
		byte[] ba = baos.toByteArray();
		String encoded = Base64.getEncoder().encodeToString(ba);
		Captcha captcha = new Captcha();
		captcha.setDate(new Date());
		captcha.setImage(encoded);
		captcha.setText(ox.getText());
		captcha.setHashCode(ipHash);
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

	public boolean isValid(long id, String text) throws UnsupportedEncodingException, NoSuchAlgorithmException {

		Captcha captcha = captchaRepo.findById(id).orElse(null);
		if (captcha == null) {
			return false;
		}

		captchaRepo.delete(captcha);
		captchaRepo.flush();

		return captcha.getHashCode().equals(getIpHash(request.getRemoteAddr()))
				&& captcha.getText().equalsIgnoreCase(text);
	}

	private String getIpHash(String ip) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		// don't need slow hashing algorithm because
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(salt.getBytes()); // salting to prevent rainbow tables
		md.update(ip.getBytes(StandardCharsets.UTF_8));
		byte[] bytes = md.digest();
		return Base64.getEncoder().encodeToString(bytes);
	}
}
