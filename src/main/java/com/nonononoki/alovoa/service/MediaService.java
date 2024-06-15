package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserAudio;
import com.nonononoki.alovoa.entity.user.UserImage;
import com.nonononoki.alovoa.entity.user.UserProfilePicture;
import com.nonononoki.alovoa.entity.user.UserVerificationPicture;
import com.nonononoki.alovoa.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

@Service
public class MediaService {

    private static final String IMAGE_DATA_START = "data:";
    public static final String MEDIA_TYPE_IMAGE_WEBP = "image/webp";
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserImageRepository userImageRepository;

    @Autowired
    private UserProfilePictureRepository userProfilePictureRepository;

    @Autowired
    private UserAudioRepository userAudioRepository;

    @Autowired
    private UserVerificationPictureRepository userVerificationPictureRepository;

    @SuppressWarnings("deprecation")
    public ResponseEntity<byte[]> getProfilePicture(UUID uuid) {
        UserProfilePicture profilePic = userProfilePictureRepository.findByUuid(uuid);
        if (profilePic == null) {
            return null;
        }
        if (profilePic.getBin() == null) {
            return getImageDataBase(profilePic.getData());
        } else {
            return getImageDataBase(profilePic.getBin(), profilePic.getBinMime());
        }
    }

    public ResponseEntity<byte[]> getVerificationPicture(UUID uuid) {
        UserVerificationPicture verificationPicture = userVerificationPictureRepository.findByUuid(uuid);
        if (verificationPicture != null) {
            return getImageDataBase(verificationPicture.getBin(), verificationPicture.getBinMime());
        }
        User user = userRepository.findByUuid(uuid);
        return getImageDataBase(user.getVerificationPicture().getBin(),
                user.getVerificationPicture().getBinMime());
    }

    public ResponseEntity<byte[]> getAudio(UUID uuid) {
        UserAudio userAudio = userAudioRepository.findByUuid(uuid);
        byte[] bytes = null;
        if (userAudio != null) {
            bytes = userAudio.getBin();
        }
        if (bytes == null) {
            User user = userRepository.findByUuid(uuid);
            bytes = user.getAudio().getBin();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("audio", "wav"));
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @SuppressWarnings("deprecation")
    public ResponseEntity<byte[]> getImage(UUID uuid) {
        UserImage image = userImageRepository.findByUuid(uuid);
        if (image.getBin() == null) {
            return getImageDataBase(image.getContent());
        } else {
            return getImageDataBase(image.getBin(), image.getBinMime());
        }
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
        MediaType mimeType = getImageMimeType(imageB64);
        return getImageDataBase(bytes, Tools.buildMimeTypeString(mimeType));
    }

    private ResponseEntity<byte[]> getImageDataBase(byte[] bytes, String mimeType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(getImageMimeType(IMAGE_DATA_START + mimeType));
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    public MediaType getImageMimeType(String imageB64) {
        MediaType mediaType;
        if (imageB64.startsWith(IMAGE_DATA_START + MEDIA_TYPE_IMAGE_WEBP)) {
            mediaType = new MediaType("image", "webp");
        } else if (imageB64.startsWith(IMAGE_DATA_START + MediaType.IMAGE_PNG)) {
            mediaType = MediaType.IMAGE_PNG;
        } else {
            mediaType = MediaType.IMAGE_JPEG;
        }
        return mediaType;
    }
}
