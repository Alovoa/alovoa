package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.model.AdminAccountDeleteDto;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.MailDto;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.service.AdminService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @PostMapping("/report/delete/{id}")
    public void deleteReport(@PathVariable long id) throws AlovoaException {
        adminService.deleteReport(id);
    }

    @PostMapping("/ban-user/{uuid}")
    public void banUser(@PathVariable UUID uuid)
            throws NumberFormatException, AlovoaException {
        adminService.banUser(uuid);
    }

    @PostMapping("/remove-images/{uuid}")
    public void removeImages(@PathVariable UUID uuid)
            throws NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, AlovoaException {
        adminService.removeImages(uuid);
    }

    @PostMapping("/remove-description/{uuid}")
    public void removeDescription(@PathVariable UUID uuid)
            throws NumberFormatException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, AlovoaException {
        adminService.removeDescription(uuid);
    }

    @PostMapping(value = "/mail/single", consumes = "application/json")
    public void sendMailSingle(@RequestBody MailDto dto) throws AlovoaException, MessagingException, IOException {
        adminService.sendMailSingle(dto);
    }

    @PostMapping(value = "/mail/all", consumes = "application/json")
    public void sendMailAll(@RequestBody MailDto dto) throws AlovoaException, MessagingException, IOException {
        adminService.sendMailAll(dto);
    }

    @PostMapping(value = "/delete-account", consumes = "application/json")
    public void deleteAccount(@RequestBody AdminAccountDeleteDto dto) throws AlovoaException {
        adminService.deleteAccount(dto);
    }

    @PostMapping("/user-exists/{email}")
    public boolean userExists(@PathVariable String email) throws AlovoaException {
        return adminService.userExists(email);
    }

    @PostMapping("/donation/add/{email}/{amount}")
    public void addDonation(@PathVariable String email, @PathVariable double amount) throws AlovoaException {
        adminService.addDonation(email, amount);
    }

    @PostMapping("/user-verification/verify/{uuid}")
    public void userVerificationVerify(@PathVariable UUID uuid)
            throws AlovoaException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        adminService.verifyVerificationPicture(uuid);
    }

    @PostMapping("/user-verification/delete/{uuid}")
    public void userVerificationDelete(@PathVariable UUID uuid)
            throws AlovoaException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        adminService.deleteVerificationPicture(uuid);
    }

    @GetMapping(path ="/profile/view/{uuid}", produces= MediaType.APPLICATION_JSON_VALUE)
    public UserDto viewProfile(@PathVariable UUID uuid) throws AlovoaException, InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, UnsupportedEncodingException, BadPaddingException,
            NoSuchAlgorithmException, InvalidKeyException {
        return adminService.viewProfile(uuid);
    }

}
