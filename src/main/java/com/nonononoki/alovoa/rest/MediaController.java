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

@RestController
@RequestMapping("/media")
public class MediaController {

    @Autowired
    private MediaService mediaService;

    public static final String URL_REQUEST_MAPPING = "/media";
    public static final String URL_PROFILE_PICTURE = "/profile-picture/";
    public static final String URL_IMAGE = "/image/";
    @GetMapping(value = URL_PROFILE_PICTURE + "{idEnc}" )
    public ResponseEntity<byte[]> getProfilePicture(@PathVariable String idEnc) throws InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException,
            InvalidKeyException {
        return mediaService.getProfilePicture(idEnc);
    }

    @GetMapping(value = URL_IMAGE + "{idEnc}/{index}")
    public ResponseEntity<byte[]> getImage(@PathVariable String idEnc, @PathVariable int index) throws InvalidAlgorithmParameterException,
            IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException,
            InvalidKeyException {
        return mediaService.getImage(idEnc, index);
    }
}
