package com.nonononoki.alovoa.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.user.UserImage;
import com.nonononoki.alovoa.entity.user.UserMiscInfo;
import com.nonononoki.alovoa.entity.user.UserSettings;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.ProfileOnboardingDto;
import com.nonononoki.alovoa.model.UserDeleteAccountDto;
import com.nonononoki.alovoa.model.UserInterestDto;
import com.nonononoki.alovoa.service.UserService;
import jakarta.mail.MessagingException;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Value("${app.media.max-size}")
    private int mediaMaxSize;

    // simple post to test session
    @PostMapping("/post")
    public void post() {
    }

    // GDPR
    @PostMapping(value = "/delete-account")
    public void deleteAccount() throws MessagingException, IOException, AlovoaException {
        userService.deleteAccountRequest();
    }

    @PostMapping(value = "/delete-account-confirm", consumes = "application/json")
    public void deleteAccountConfirm(@RequestBody UserDeleteAccountDto dto) throws NoSuchAlgorithmException,
            MessagingException, IOException, AlovoaException {
        userService.deleteAccountConfirm(dto);
    }

    @GetMapping(value = "/userdata/{idEnc}")
    public ResponseEntity<Resource> getUserdata(@PathVariable String idEnc) throws JsonProcessingException,
            UnsupportedEncodingException, AlovoaException, NumberFormatException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException {
        return userService.getUserdata(idEnc);
    }

    @PostMapping(value = "/onboarding", consumes = "application/json")
    public void onboarding(@RequestBody ProfileOnboardingDto dto) throws AlovoaException, IOException {
        userService.onboarding(dto);
    }

    @PostMapping(value = "/delete/profile-picture")
    public void deleteProfilePicture() throws AlovoaException {
        userService.deleteProfilePicture();
    }

    @PostMapping(value = "/update/profile-picture", consumes = "text/plain")
    public void updateProfilePicture(@RequestBody String imageB64) throws AlovoaException, IOException {
        if (Tools.getBase64Size(imageB64) > mediaMaxSize) {
            throw new AlovoaException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
        }
        userService.updateProfilePicture(imageB64);
    }

    @PostMapping(value = "/update/verification-picture", consumes = "text/plain")
    public void updateVerificationPicture(@RequestBody String imageB64) throws AlovoaException, IOException {
        if (Tools.getBase64Size(imageB64) > mediaMaxSize) {
            throw new AlovoaException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
        }
        userService.updateVerificationPicture(imageB64);
    }

    @PostMapping(value = "/update/verification-picture/upvote/{userIdEnc}")
    public void upvoteVerificationPicture(@PathVariable String userIdEnc) throws AlovoaException, IOException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException,
            BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        userService.upvoteVerificationPicture(userIdEnc);
    }

    @PostMapping(value = "/update/verification-picture/downvote/{userIdEnc}")
    public void downvoteVerificationPicture(@PathVariable String userIdEnc) throws AlovoaException, IOException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException,
            BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        userService.downvoteVerificationPicture(userIdEnc);
    }

    @GetMapping(value = "/get/audio/{userIdEnc}")
    public String getAudio(@PathVariable String userIdEnc) throws NumberFormatException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException {
        return userService.getAudio(userIdEnc);
    }

    @PostMapping(value = "/delete/audio")
    public void deleteAudio() throws AlovoaException {
        userService.deleteAudio();
    }

    @PostMapping(value = "/update/audio/{mimeType}", consumes = "text/plain")
    public void updateAudio(@RequestBody String audioB64, @PathVariable String mimeType) throws AlovoaException,
            UnsupportedAudioFileException, IOException {
        if (Tools.getBase64Size(audioB64) > mediaMaxSize) {
            throw new AlovoaException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
        }
        userService.updateAudio(audioB64, mimeType);
    }

    @PostMapping(value = "/update/description", consumes = "text/plain")
    public void updateDescription(@RequestBody(required = false) String description) throws AlovoaException {
        userService.updateDescription(description);
    }

    @PostMapping("/update/intention/{intention}")
    public void updateIntention(@PathVariable long intention) throws AlovoaException {
        userService.updateIntention(intention);
    }

    @PostMapping("/update/min-age/{minAge}")
    public void updateMinAge(@PathVariable int minAge) throws AlovoaException {
        userService.updateMinAge(minAge);
    }

    @PostMapping("/update/max-age/{maxAge}")
    public void updateMaxAge(@PathVariable int maxAge) throws AlovoaException {
        userService.updateMaxAge(maxAge);
    }

    @PostMapping("/update/preferedGender/{genderId}/{activated}")
    public void updatePreferedGenders(@PathVariable int genderId, @PathVariable String activated) throws AlovoaException {
        userService.updatePreferedGender(genderId, Tools.binaryStringToBoolean(activated));
    }

    @PostMapping("/update/misc-info/{infoValue}/{activated}")
    public Set<UserMiscInfo> updateMiscInfo(@PathVariable long infoValue, @PathVariable String activated) throws AlovoaException {
        return userService.updateUserMiscInfo(infoValue, Tools.binaryStringToBoolean(activated));
    }

    @PostMapping("/interest/add/{value}")
    public void addInterest(@PathVariable String value) throws AlovoaException {
        userService.addInterest(value);
    }

    @PostMapping("/interest/delete/{value}")
    public void deleteInterest(@PathVariable String value) throws AlovoaException {
        userService.deleteInterest(value);
    }

    @GetMapping(value = "/interest/autocomplete/{name}")
    public List<UserInterestDto> interestAutocomplete(@PathVariable String name) throws AlovoaException {
        return userService.getInterestAutocomplete(name);
    }

    @PostMapping("/accent-color/update/{accentColor}")
    public void updateAccentColor(@PathVariable String accentColor) throws AlovoaException {
        userService.updateAccentColor(accentColor);
    }

    @PostMapping("/ui-design/update/{uiDesign}")
    public void updateUiDesign(@PathVariable String uiDesign) throws AlovoaException {
        userService.updateUiDesign(uiDesign);
    }

    @PostMapping("/show-zodiac/update/{showZodiac}")
    public void updateZodiac(@PathVariable int showZodiac) throws AlovoaException {
        userService.updateShowZodiac(showZodiac);
    }

    @PostMapping("/units/update/{units}")
    public void updateUnits(@PathVariable int units) throws AlovoaException {
        userService.updateUnits(units);
    }

    @PostMapping(value = "/image/add", consumes = "text/plain")
    public List<UserImage> addImage(@RequestBody String imageB64) throws AlovoaException, IOException {
        if (Tools.getBase64Size(imageB64) > mediaMaxSize) {
            throw new AlovoaException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
        }
        return userService.addImage(imageB64);
    }

    @PostMapping("/image/delete/{imageId}")
    public void deleteImage(@PathVariable long imageId) throws AlovoaException {
        userService.deleteImage(imageId);
    }

    @PostMapping(value = "/like/{idEnc}")
    public void likeUser(@PathVariable String idEnc) throws AlovoaException, GeneralSecurityException, IOException,
            JoseException {
        userService.likeUser(idEnc, null);
    }

    @PostMapping(value = "/like/{idEnc}/{message}")
    public void likeUser(@PathVariable String idEnc, @PathVariable String message) throws AlovoaException,
            GeneralSecurityException, IOException, JoseException {
        userService.likeUser(idEnc, message);
    }

    @PostMapping(value = "/hide/{idEnc}")
    public void hideUser(@PathVariable String idEnc) throws NumberFormatException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, AlovoaException {
        userService.hideUser(idEnc);
    }

    @PostMapping(value = "/block/{idEnc}")
    public void blockUser(@PathVariable String idEnc) throws NumberFormatException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, AlovoaException {
        userService.blockUser(idEnc);
    }

    @PostMapping(value = "/unblock/{idEnc}")
    public void unblockUser(@PathVariable String idEnc) throws NumberFormatException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, AlovoaException {
        userService.unblockUser(idEnc);
    }

    @PostMapping(value = "/report/{idEnc}", consumes = "text/plain")
    public void reportUser(@PathVariable String idEnc, @RequestBody String comment) throws NumberFormatException,
            InvalidKeyException, UnsupportedEncodingException, NoSuchAlgorithmException, IllegalBlockSizeException,
            BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, AlovoaException {
        userService.reportUser(idEnc, comment);
    }

    @GetMapping(value = "/status/new-alert")
    public boolean newAlert() throws AlovoaException {
        return userService.hasNewAlert();
    }

    @RequestMapping(value = "/status/new-alert/{lang}")
    public boolean newAlert2(@PathVariable String lang) throws AlovoaException {
        return userService.hasNewAlert(lang);
    }

    @GetMapping(value = "/status/new-message")
    public boolean newMessage() throws AlovoaException {
        return userService.hasNewMessage();
    }

}
