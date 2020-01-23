package com.nonononoki.alovoa.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.Gender;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.UserIntention;
import com.nonononoki.alovoa.entity.UserInterest;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.repo.UserInterestRepository;
import com.nonononoki.alovoa.repo.UserRepository;

@Service
public class UserService {

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepo;
	
	@Autowired
	private GenderRepository genderRepo;
	
	@Autowired
	private UserIntentionRepository userIntentionRepo;
	
	@Autowired
	private UserInterestRepository userInterestRepo;

	@Value("${app.age.min}")
	private int minAge;
	
	@Value("${app.age.max}")
	private int maxAge;
	
	@Value("${app.profile.image.size}")
	private int imageSize;
	
	@Value("${app.profile.image.length}")
	private int imageLength;

	@Value("${app.profile.description.size}")
	private int descriptionSize;
	
	public void updateProfilePicture(String imgB64) throws Exception {
		if(imgB64.length() > Tools.BASE64FACTOR * Tools.MILLION * imageSize) {
			throw new Exception("");
		}
		
		User user = authService.getCurrentUser();
		String newImgB64 = adjustPicture(imgB64);
		user.setProfilePicture(newImgB64);
		userRepo.save(user);
	}
	
	public void updateDescription(String description) throws Exception {
		if(description.length() > descriptionSize) {
			throw new Exception("");
		}
		User user = authService.getCurrentUser();
		user.setDescription(description);
		userRepo.save(user);	
	}
	
	public void updateIntention(long intention) {
		//TODO Limit intention changing time
		User user = authService.getCurrentUser();
		UserIntention i = userIntentionRepo.findById(intention).orElse(null);
		user.setIntention(i);
		user.setIntentionChangeDate(new Date());
		userRepo.save(user);	
	}
	
	public void updateMinAge(int userMinAge) throws Exception {
		if(userMinAge < minAge) {
			userMinAge = minAge;
		}
		User user = authService.getCurrentUser();
		user.setPreferedMinAge(userMinAge);
		userRepo.save(user);	
	}
	
	public void updateMaxAge(int userMaxAge) throws Exception {
		if(userMaxAge > maxAge) {
			userMaxAge = maxAge;
		}
		User user = authService.getCurrentUser();
		user.setPreferedMaxAge(userMaxAge);
		userRepo.save(user);	
	}
	
	public void updateInterest(long interest, boolean activated) {
		User user = authService.getCurrentUser();
		List<UserInterest> list = user.getInterests();
		UserInterest i = userInterestRepo.findById(interest).orElse(null);
		if(activated) {
			if(list.contains(i)) {
				list.remove(i);
			}
		} else {
			if(!list.contains(i)) {
				list.add(i);
			}
		}
		user.setInterests(list);
		userRepo.save(user);	
	}
	
	
	public void updatePreferedGender(long genderId, boolean activated) {
		User user = authService.getCurrentUser();
		Set<Gender> list = user.getPreferedGenders();
		Gender g = genderRepo.findById(genderId).orElse(null);
		if(activated) {
			if(!list.contains(g)) {
				list.add(g);
			}
		} else {
			if(list.contains(g)) {
				list.remove(g);
			}
		}
		user.setPreferedGenders(list);
		userRepo.save(user);	
	}

	private String adjustPicture(String imgB64) throws IOException {
		// convert b64 to bufferedimage
		BufferedImage image = null;
		byte[] decodedBytes = Base64.getDecoder().decode(stripImageType(imgB64));
		ByteArrayInputStream bis = new ByteArrayInputStream(decodedBytes);
		image = ImageIO.read(bis);

		if (image.getWidth() != image.getHeight()) {

			int idealLength = image.getHeight();
			boolean heightShorter = true;
			if (image.getWidth() < image.getHeight()) {
				heightShorter = false;
				idealLength = image.getWidth();
			}

			// cut to a square
			int x = 0;
			int y = 0;

			if (heightShorter) {
				y = 0;
				x = image.getWidth() / 2 - image.getHeight() / 2;
			} else {
				x = 0;
				y = image.getHeight() / 2 - image.getWidth() / 2;
			}
			image = image.getSubimage(x, y, idealLength, idealLength);
		}

		
		if (image.getWidth() > imageSize) {
			// scale image if it's too big
			BufferedImage scaledImage = new BufferedImage(imageLength, imageLength, image.getType());
            Graphics2D graphics2D = scaledImage.createGraphics();
            
            //chose one
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            //graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            //graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            graphics2D.drawImage(image, 0, 0, imageLength, imageLength, null);
            graphics2D.dispose();
            image = scaledImage;
		}
		
		
		//image to b64
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "png", out);
		byte[] bytes = out.toByteArray();
		String base64bytes = Base64.getEncoder().encodeToString(bytes);
		String src = Tools.B64IMAGEPREFIX + base64bytes;
		return src;

	}
	
	private String stripImageType(String s) {
		if(s.contains(",")) {
			return s.split(",")[1];
		}
		return s;
	}

}
