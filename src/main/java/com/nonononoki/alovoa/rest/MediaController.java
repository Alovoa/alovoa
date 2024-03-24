package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@RestController
@RequestMapping("/media")
public class MediaController {

    @Autowired
    private MediaService mediaService;

    public static final String URL_REQUEST_MAPPING = "/media";
    public static final String URL_PROFILE_PICTURE = "/profile-picture/";
    public static final String URL_IMAGE = "/image/";
    @GetMapping(value = URL_PROFILE_PICTURE + "{uuid}" )
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable UUID uuid) throws InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException,
            InvalidKeyException {
        return mediaService.getProfilePicture(uuid);
    }

    @GetMapping(value = URL_IMAGE + "{uuid}")
    public ResponseEntity<byte[]> getImage(@PathVariable UUID uuid) throws InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException,
            InvalidKeyException {
        return mediaService.getImage(uuid);
    }
}
