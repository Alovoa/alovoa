package com.nonononoki.alovoa.html;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserIntentionRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Controller
public class ProfileResource {

    public static final String URL = "/profile";
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private GenderRepository genderRepo;
    @Autowired
    private UserIntentionRepository userIntentionRepo;
    @Autowired
    private TextEncryptorConverter textEncryptor;
    @Value("${app.profile.image.max}")
    private int imageMax;
    @Value("${app.image.max-size}")
    private int mediaMaxSize;
    @Value("${app.interest.max}")
    private int interestMaxSize;
    @Value("${app.intention.delay}")
    private long intentionDelay;
    @Value("${app.domain}")
    private String domain;
    @Value("${app.referral.max}")
    private int maxReferrals;
    @Value("${app.search.ignore-intention}")
    private boolean ignoreIntention;

    @GetMapping(URL)
    public ModelAndView profile() throws AlovoaException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            UnsupportedEncodingException {

        User user = authService.getCurrentUser(true);
        if (user.isAdmin()) {
            return new ModelAndView("redirect:" + AdminResource.URL);
        } else {
            int age = Tools.calcUserAge(user);
            boolean isLegal = age >= Tools.AGE_LEGAL;
            int referralsLeft = maxReferrals - user.getNumberReferred();
            ModelAndView mav = new ModelAndView("profile");
            mav.addObject("user", UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                    .currentUser(user).user(user).userService(userService).build()));
            mav.addObject("genders", genderRepo.findAll());
            mav.addObject("intentions", userIntentionRepo.findAll());
            mav.addObject("imageMax", imageMax);
            mav.addObject("isLegal", isLegal);
            mav.addObject("mediaMaxSize", mediaMaxSize);
            mav.addObject("interestMaxSize", interestMaxSize);
            mav.addObject("domain", domain);
            mav.addObject("referralsLeft", referralsLeft);

            boolean showIntention = false;
            Date now = new Date();
            if (user.getDates().getIntentionChangeDate() == null
                    || now.getTime() >= user.getDates().getIntentionChangeDate().getTime() + intentionDelay) {
                showIntention = true;
            }
            mav.addObject("showIntention", showIntention);
            return mav;
        }
    }
}
