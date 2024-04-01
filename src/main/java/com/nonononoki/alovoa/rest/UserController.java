package com.nonononoki.alovoa.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.user.UserImage;
import com.nonononoki.alovoa.entity.user.UserMiscInfo;
import com.nonononoki.alovoa.model.*;
import com.nonononoki.alovoa.service.UserService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserService userService;
    @Value("${app.audio.max-size}")
    private int audioMaxSize;
    @Value("${app.image.max-size}")
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

    @GetMapping(value = "/userdata/{uuid}")
    public ResponseEntity<Resource> getUserdata(@PathVariable UUID uuid) throws JsonProcessingException,
            AlovoaException, NumberFormatException {
        return userService.getUserdata(uuid);
    }

    @PostMapping(value = "/onboarding", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public void onboarding(@RequestParam("file") MultipartFile file, @RequestParam("data") String dto)
            throws AlovoaException, IOException {
        byte[] bytes = file.getBytes();
        if (bytes.length > mediaMaxSize) {
            throw new AlovoaException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
        }
        userService.onboarding(bytes, objectMapper.readValue(dto, ProfileOnboardingDto.class));
    }

    @PostMapping(value = "/update/profile-picture", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public void updateProfilePicture(@RequestParam("file") MultipartFile file, @RequestParam("mime") String mimeType)
            throws AlovoaException, IOException {
        byte[] bytes = file.getBytes();
        if (bytes.length > mediaMaxSize) {
            throw new AlovoaException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
        }
        userService.updateProfilePicture(bytes, mimeType);
    }

    @PostMapping(value = "/update/verification-picture")
    public void updateVerificationPicture(@RequestParam("file") MultipartFile file, @RequestParam("mime") String mimeType)
            throws AlovoaException, IOException {
        byte[] bytes = file.getBytes();
        if (bytes.length > mediaMaxSize) {
            throw new AlovoaException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
        }
        userService.updateVerificationPicture(bytes, mimeType);
    }

    @PostMapping(value = "/update/verification-picture/upvote/{uuid}")
    public void upvoteVerificationPicture(@PathVariable UUID uuid) throws AlovoaException, IOException,
            InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException,
            BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        userService.upvoteVerificationPicture(uuid);
    }

    @PostMapping(value = "/update/verification-picture/downvote/{uuid}")
    public void downvoteVerificationPicture(@PathVariable UUID uuid) throws AlovoaException {
        userService.downvoteVerificationPicture(uuid);
    }

    @GetMapping(value = "/get/audio/{uuid}")
    public String getAudio(@PathVariable UUID uuid) throws NumberFormatException, AlovoaException {
        return userService.getAudio(uuid);
    }

    @PostMapping(value = "/delete/audio")
    public void deleteAudio() throws AlovoaException {
        userService.deleteAudio();
    }

    @PostMapping(value = "/update/audio/{mimeType}", consumes = "text/plain")
    public void updateAudio(@RequestParam("file") MultipartFile file, @RequestParam("mime") String mimeType)
            throws AlovoaException, UnsupportedAudioFileException, IOException {
        byte[] bytes = file.getBytes();
        if (bytes.length > audioMaxSize) {
            throw new AlovoaException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
        }
        userService.updateAudio(bytes, mimeType);
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

    @PostMapping("/show-zodiac/update/{showZodiac}")
    public void updateZodiac(@PathVariable int showZodiac) throws AlovoaException {
        userService.updateShowZodiac(showZodiac);
    }

    @PostMapping("/units/update/{units}")
    public void updateUnits(@PathVariable int units) throws AlovoaException {
        userService.updateUnits(units);
    }

    @PostMapping(value = "/image/add", consumes = { MediaType.MULTIPART_FORM_DATA_VALUE })
    public List<UserImageDto> addImage(@RequestParam("file") MultipartFile file, @RequestParam("mime") String mimeType)
            throws AlovoaException, IOException {
        byte[] bytes = file.getBytes();
        if (bytes.length > mediaMaxSize) {
            throw new AlovoaException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
        }
        return userService.addImage(bytes, mimeType);
    }

    @PostMapping("/image/delete/{imageId}")
    public void deleteImage(@PathVariable long imageId) throws AlovoaException {
        userService.deleteImage(imageId);
    }

    @PostMapping(value = "/like/{uuid}")
    public void likeUser(@PathVariable UUID uuid) throws AlovoaException {
        userService.likeUser(uuid, null);
    }

    @PostMapping(value = "/like/{uuid}/{message}")
    public void likeUser(@PathVariable UUID uuid, @PathVariable String message) throws AlovoaException {
        userService.likeUser(uuid, message);
    }

    @PostMapping(value = "/hide/{uuid}")
    public void hideUser(@PathVariable UUID uuid) throws NumberFormatException, AlovoaException {
        userService.hideUser(uuid);
    }

    @PostMapping(value = "/block/{uuid}")
    public void blockUser(@PathVariable UUID uuid) throws NumberFormatException, AlovoaException {
        userService.blockUser(uuid);
    }

    @PostMapping(value = "/unblock/{uuid}")
    public void unblockUser(@PathVariable UUID uuid) throws NumberFormatException, AlovoaException {
        userService.unblockUser(uuid);
    }

    @PostMapping(value = "/report/{uuid}", consumes = "text/plain")
    public void reportUser(@PathVariable UUID uuid, @RequestBody String comment) throws NumberFormatException,
            AlovoaException {
        userService.reportUser(uuid, comment);
    }

    @GetMapping(value = "/status/new-alert")
    public boolean newAlert() throws AlovoaException {
        return userService.hasNewAlert();
    }

    @GetMapping(value = "/status/new-alert/{lang}")
    public boolean newAlert2(@PathVariable String lang) throws AlovoaException {
        return userService.hasNewAlert(lang);
    }

    @GetMapping(value = "/status/new-message")
    public boolean newMessage() throws AlovoaException {
        return userService.hasNewMessage();
    }

}
