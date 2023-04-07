package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserDonation;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.DonationBmac;
import com.nonononoki.alovoa.model.DonationDto;
import com.nonononoki.alovoa.model.DonationKofi;
import com.nonononoki.alovoa.repo.UserDonationRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

@Service
public class DonateService {

    private static final int FILTER_RECENT = 1;
    private static final int FILTER_AMOUNT = 2;
    private static final Logger logger = LoggerFactory.getLogger(DonateService.class);
    private static final String KOFI_TEST_TRANSACTION_ID = "1234-1234-1234-1234";
    private static final String KOFI_TEST_EMAIL = "john@example.com";
    private static final String BMAC_TEST_EMAIL = "test@example.com";
    @Autowired
    private UserDonationRepository userDonationRepo;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserService userService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private TextEncryptorConverter textEncryptor;
    @Value("${app.donate.users.max}")
    private int maxEntries;
    @Value("${spring.profiles.active}")
    private String profile;
    @Value("${app.donate.kofi.key}")
    private String kofiKey;
    @Value("${app.donate.bmac.key}")
    private String bmacKey;
    // private static final double BMAC_AMOUNT_FACTOR = 1.0; // 0.95;

    public List<DonationDto> filter(int filter) throws AlovoaException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            UnsupportedEncodingException {
        List<DonationDto> donationsToDtos = null;

        User user = authService.getCurrentUser(true);

        int ageLegal = Tools.AGE_LEGAL;

        int age = Tools.calcUserAge(user);
        boolean isLegalAge = age >= ageLegal;
        int minAge = user.getPreferedMinAge();
        int maxAge = user.getPreferedMaxAge();

        if (isLegalAge && minAge < ageLegal) {
            minAge = ageLegal;
        }
        if (!isLegalAge && maxAge >= ageLegal) {
            maxAge = ageLegal - 1;
        }

        Date minDate = Tools.ageToDate(maxAge);
        Date maxDate = Tools.ageToDate(minAge);

        if (filter == FILTER_RECENT) {
            donationsToDtos = DonationDto.donationsToDtos(userDonationRepo
                            .findTop20ByUserDatesDateOfBirthGreaterThanEqualAndUserDatesDateOfBirthLessThanEqualOrderByDateDesc(
                                    minDate, maxDate),
                    user, userService, textEncryptor, maxEntries);
        } else if (filter == FILTER_AMOUNT) {
            donationsToDtos = DonationDto.usersToDtos(userRepo.usersDonate(minDate, maxDate), user, userService, textEncryptor,
                    maxEntries);
        } else {
            throw new AlovoaException("filter_not_found");
        }

        return donationsToDtos;
    }

    public void donationReceivedKofi(DonationKofi donation, String key) throws UnknownHostException, MalformedURLException {

        try {
            logger.info(objectMapper.writeValueAsString(donation));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        donation.setEmail(Tools.cleanEmail(donation.getEmail()));

        if (kofiKey.equals(key)) {

            Date now = new Date();

            if (profile.equals(Tools.PROD) && (KOFI_TEST_TRANSACTION_ID.equals(donation.getKofi_transaction_id())
                    || donation.getEmail() != null && KOFI_TEST_EMAIL.equalsIgnoreCase(donation.getEmail()))) {
                logger.warn("Donation is not valid");
                return;
            }

            User u = null;

            if (donation.getFrom_name() != null) {
                u = userRepo.findByEmail(Tools.cleanEmail(donation.getFrom_name()));
            }

            if (u == null && donation.getMessage() != null) {
                u = userRepo.findByEmail(Tools.cleanEmail(donation.getMessage()));
            }

            // in case user forgot, check their Ko-fi email address just in case
            if (u == null && donation.getEmail() != null) {
                u = userRepo.findByEmail(Tools.cleanEmail(donation.getEmail()));
            }

            if (u != null) {
                double amount = Double.parseDouble(donation.getAmount());
                UserDonation userDonation = new UserDonation();
                userDonation.setAmount(amount);
                userDonation.setDate(now);
                userDonation.setUser(u);
                u.getDonations().add(userDonation);
                u.setTotalDonations(u.getTotalDonations() + amount);
                u.getDates().setLatestDonationDate(new Date());
                userRepo.saveAndFlush(u);
            }
        } else {
            logger.error("Invalid key");
        }
    }

    public void donationReceivedBmac(DonationBmac data, String key) throws UnknownHostException, MalformedURLException {

        try {
            logger.info(objectMapper.writeValueAsString(data));
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        if (bmacKey.equals(key)) {

            Date now = new Date();
            DonationBmac.DonationBmacResponse donation = data.getResponse();

            if (profile.equals(Tools.PROD) && BMAC_TEST_EMAIL.equalsIgnoreCase(donation.getSupporter_email())) {
                logger.warn("Donation is not valid");
                return;
            }

            User u = null;

            if (donation.getSupporter_name() != null) {
                u = userRepo.findByEmail(Tools.cleanEmail(donation.getSupporter_name()));
            }

            if (u == null && donation.getSupporter_message() != null) {
                u = userRepo.findByEmail(Tools.cleanEmail(donation.getSupporter_message()));
            }

            if (u == null && donation.getSupporter_email() != null) {
                u = userRepo.findByEmail(Tools.cleanEmail(donation.getSupporter_email()));
            }

            if (u != null) {
                UserDonation userDonation = new UserDonation();
                double amount = donation.getTotal_amount();// * BMAC_AMOUNT_FACTOR;
                // amount = (double) Math.round(amount * 100) / 100;
                userDonation.setAmount(amount);
                userDonation.setDate(now);
                userDonation.setUser(u);
                u.getDonations().add(userDonation);
                u.setTotalDonations(u.getTotalDonations() + amount);
                u.getDates().setLatestDonationDate(new Date());
                userRepo.saveAndFlush(u);
            }
        } else {
            logger.error("Invalid key");
        }
    }

}
