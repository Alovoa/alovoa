package com.nonononoki.alovoa.html;

import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDonation;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.UserRepository;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Controller
public class SearchResource {

    public static final String URL = "/search";
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private TextEncryptorConverter textEncryptor;
    @Value("${app.donation.modulus}")
    private int donationModulus;
    @Value("${app.donation.popup.time}")
    private int donationPopupTime;
    @Value("${app.search.max.distance}")
    private int searchMaxDistance;


    @GetMapping(URL)
    public ModelAndView search() throws AlovoaException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            UnsupportedEncodingException {
        User user = authService.getCurrentUser(true);

        if (!user.isAdmin() && user.getProfilePicture() == null && user.getDescription() == null) {
            return new ModelAndView("redirect:" + ProfileOnboardingResource.URL);
        }

        user.setNumberSearches(user.getNumberSearches() + 1);
        userRepo.saveAndFlush(user);

        boolean showDonationPopup = false;
        if (user.getNumberSearches() % donationModulus == 0) {

            List<UserDonation> userDonations = user.getDonations();
            if (userDonations != null && !userDonations.isEmpty()) {
                UserDonation lastDonation = Collections.max(userDonations, Comparator.comparing(UserDonation::getDate));
                Date date = new Date();
                long ms = lastDonation.getDate().getTime();
                long diff = date.getTime() - ms;

                if (diff >= donationPopupTime) {
                    showDonationPopup = true;
                }
            } else {
                showDonationPopup = true;
            }
        }

        ModelAndView mav = new ModelAndView("search");
        mav.addObject("user", UserDto.userToUserDto(user, user, userService, textEncryptor, UserDto.NO_MEDIA));
        mav.addObject("showDonationPopup", showDonationPopup);
        mav.addObject("searchMaxDistance", searchMaxDistance);
        return mav;
    }
}
