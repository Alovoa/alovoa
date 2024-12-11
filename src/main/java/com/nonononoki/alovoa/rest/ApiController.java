package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.DonationDto;
import com.nonononoki.alovoa.model.MessageDto;
import com.nonononoki.alovoa.model.SearchDto;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.SearchService;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class ApiController {

    @Autowired
    private SearchService searchService;

    @Autowired
    private AuthService authService;

    @Autowired
    private DonateController donateController;

    @Autowired
    private MessageController messageController;

    @Autowired
    private SearchController searchController;

    @SuppressWarnings("unchecked")
    @GetMapping("/donate/recent/{filter}")
    public DonationDtoListModel donateRecent(Model model, @PathVariable int filter) throws
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        Map<String, Object> map = donateController.filterRecentModel(model, filter).asMap();
        List<DonationDto> donations = (List<DonationDto>) map.get("donations");
        return DonationDtoListModel.builder().list(donations).build();
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/message/update/{convoId}/{first}")
    public MessageDtoListModel messageUpdate(@PathVariable long convoId, @PathVariable int first)
            throws AlovoaException {
        Map<String, Object> map = messageController.getMessagesModel(null, convoId, first).asMap();
        boolean show = (boolean) map.get("show");
        if (show) {
            return MessageDtoListModel.builder().list((List<MessageDto>) map.get("messages")).build();
        } else {
            return null;
        }
    }

    @PostMapping("/search/users")
    public SearchDto searchUsers(Model model, @RequestBody SearchService.SearchParams params) throws InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        return searchService.searchComplete(params);
    }

    @Deprecated
    @GetMapping("/search/users/{latitude}/{longitude}/{distance}/{search}")
    public SearchDto searchUsers(@PathVariable Double latitude, @PathVariable Double longitude,
                                 @PathVariable int distance, @PathVariable int search) throws InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        Map<String, Object> map = searchController.getUsersModel(null, latitude, longitude, distance, search).asMap();
        return (SearchDto) map.get("dto");
    }

    @Deprecated
    @GetMapping("/search/users/default")
    public SearchDto searchUsersDefault() throws InvalidKeyException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        Map<String, Object> map = searchController.getUsersDefaultModel(null).asMap();
        return (SearchDto) map.get("dto");
    }

    @Data
    @Builder
    public static class DonationDtoListModel {
        List<DonationDto> list;
    }

    @Data
    @Builder
    public static class MessageDtoListModel {
        List<MessageDto> list;
    }

}
