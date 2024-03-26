package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserImage;
import com.nonononoki.alovoa.repo.UserImageRepository;
import com.nonononoki.alovoa.repo.UserRepository;
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
import java.util.UUID;

@Service
public class MediaService {

    private static final String IMAGE_DATA_START = "data:";
    private static final String MEDIA_TYPE_IMAGE_WEBP = "image/webp";
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserImageRepository userImageRepository;

    public ResponseEntity<byte[]> getProfilePicture(UUID uuid) {
        User user = userRepository.findByUuid(uuid);
        return getImageDataBase(user.getProfilePicture().getData());
    }

    public ResponseEntity<byte[]> getVerificationPicture(UUID uuid) {
        User user = userRepository.findByUuid(uuid);
        return getImageDataBase(user.getVerificationPicture().getData());
    }

    public ResponseEntity<byte[]> getAudio(UUID uuid) {
        User user = userRepository.findByUuid(uuid);
        byte[] bytes = getBase64Data(user.getAudio().getData());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("audio", "wav"));
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    public ResponseEntity<byte[]> getImage(UUID uuid) {
        UserImage image = userImageRepository.findByUuid(uuid);
        return getImageDataBase(image.getContent());
    }

    private byte[] getBase64Data(String base64) {
        String data = base64;
        if (data.contains(",")) {
            data = data.split(",", 2)[1];
        }
        return Base64.getDecoder().decode(data);
    }

    private ResponseEntity<byte[]> getImageDataBase(String imageB64) {
        byte[] bytes = getBase64Data(imageB64);
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
