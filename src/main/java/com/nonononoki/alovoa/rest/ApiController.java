package com.nonononoki.alovoa.rest;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.DonationDto;
import com.nonononoki.alovoa.model.MessageDto;
import com.nonononoki.alovoa.model.SearchDto;

import lombok.Builder;
import lombok.Data;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    @Autowired
    private DonateController donateController;

    @Autowired
    private MessageController messageController;

    @Autowired
    private SearchController searchController;

    @SuppressWarnings("unchecked")
    @GetMapping("/donate/recent/{filter}")
    public DonationDtoListModel donateRecent(Model model, @PathVariable int filter) throws JsonProcessingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        Map<String, Object> map = donateController.filterRecentModel(model, filter).asMap();
        List<DonationDto> donations = (List<DonationDto>) map.get("donations");
        return DonationDtoListModel.builder().list(donations).build();
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/message/update/{convoId}/{first}")
    public MessageDtoListModel messageUpdate(@PathVariable long convoId, @PathVariable int first)
            throws JsonProcessingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            UnsupportedEncodingException, AlovoaException {
        Map<String, Object> map = messageController.getMessagesModel(null, convoId, first).asMap();
        boolean show = (boolean) map.get("show");
        if (show) {
            return MessageDtoListModel.builder().list((List<MessageDto>) map.get("messages")).build();
        } else {
            return null;
        }
    }

    @GetMapping("/search/users/{latitude}/{longitude}/{distance}/{search}")
    public SearchDto searchUsers(@PathVariable Double latitude, @PathVariable Double longitude,
                                 @PathVariable int distance, @PathVariable int search) throws JsonProcessingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        Map<String, Object> map = searchController.getUsersModel(null, latitude, longitude, distance, search).asMap();
        return (SearchDto) map.get("dto");
    }

    @GetMapping("/search/users/default")
    public SearchDto searchUsersDefault() throws JsonProcessingException, InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        Map<String, Object> map = searchController.getUsersDefaultModel(null).asMap();
        return (SearchDto) map.get("dto");
    }

    @Data
    @Builder
    private static class DonationDtoListModel {
        List<DonationDto> list;
    }

    @Data
    @Builder
    private static class MessageDtoListModel {
        List<MessageDto> list;
    }

}
