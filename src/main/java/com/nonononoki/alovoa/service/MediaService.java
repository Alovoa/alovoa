package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class MediaService {

    private static final String IMAGE_DATA_START = "data:";
    private static final String MEDIA_TYPE_IMAGE_WEBP = "image/webp";
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaService.class);

    @Autowired
    private UserService userService;

    public ResponseEntity<byte[]> getProfilePicture(String idEnc) throws InvalidAlgorithmParameterException, IllegalBlockSizeException,
            NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        User user = userService.encodedIdToUser(idEnc);
        return getImageDataBase(user.getProfilePicture().getData());
    }

    public ResponseEntity<byte[]> getImage(String idEnc, int id) throws InvalidAlgorithmParameterException, IllegalBlockSizeException,
            NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        User user = userService.encodedIdToUser(idEnc);
        UserImage img = user.getImages().get(id);
        return getImageDataBase(img.getContent());
    }

    private ResponseEntity<byte[]> getImageDataBase(String imageB64) {
        String data = imageB64;
        if (data.contains(",")) {
            data = data.split(",", 2)[1];
        }
        byte[] bytes = Base64.getDecoder().decode(data);
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType;
        if (imageB64.startsWith(IMAGE_DATA_START + MEDIA_TYPE_IMAGE_WEBP)) {
            mediaType = new MediaType("image", "webp");
        } else if (imageB64.startsWith(IMAGE_DATA_START + MediaType.IMAGE_PNG)) {
            mediaType = MediaType.IMAGE_PNG;
        } else {
            mediaType = MediaType.IMAGE_JPEG;
        }
        headers.setContentType(mediaType);
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
