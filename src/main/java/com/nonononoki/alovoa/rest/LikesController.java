package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserLike;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.repo.UserLikeRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Like and mutual-like endpoints for OKCupid-style "Matches" surfaces.
 */
@RestController
@RequestMapping("/api/v1/likes")
public class LikesController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserLikeRepository likeRepo;

    @Autowired
    private UserService userService;

    @Value("${app.search.ignore-intention}")
    private boolean ignoreIntention;

    @GetMapping("/received")
    public ResponseEntity<?> getReceivedLikes(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "false") boolean unmatchedOnly) {
        try {
            User currentUser = authService.getCurrentUser(true);
            List<UserLike> likes = likeRepo.findByUserTo(currentUser).stream()
                    .sorted(Comparator.comparing(UserLike::getDate).reversed())
                    .collect(Collectors.toList());

            List<User> users = new ArrayList<>();
            for (UserLike like : likes) {
                User sender = like.getUserFrom();
                if (sender == null) {
                    continue;
                }
                if (unmatchedOnly && isMutualLike(currentUser, sender)) {
                    continue;
                }
                users.add(sender);
            }

            return ResponseEntity.ok(Map.of(
                    "users", toDtos(deduplicate(users), currentUser, limit),
                    "count", users.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sent")
    public ResponseEntity<?> getSentLikes(@RequestParam(defaultValue = "50") int limit) {
        try {
            User currentUser = authService.getCurrentUser(true);
            List<User> users = likeRepo.findByUserFrom(currentUser).stream()
                    .sorted(Comparator.comparing(UserLike::getDate).reversed())
                    .map(UserLike::getUserTo)
                    .filter(u -> u != null)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "users", toDtos(deduplicate(users), currentUser, limit),
                    "count", users.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mutual")
    public ResponseEntity<?> getMutualLikes(@RequestParam(defaultValue = "50") int limit) {
        try {
            User currentUser = authService.getCurrentUser(true);
            List<User> users = likeRepo.findByUserFrom(currentUser).stream()
                    .sorted(Comparator.comparing(UserLike::getDate).reversed())
                    .map(UserLike::getUserTo)
                    .filter(u -> u != null && isMutualLike(currentUser, u))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "users", toDtos(deduplicate(users), currentUser, limit),
                    "count", users.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getLikeSummary() {
        try {
            User currentUser = authService.getCurrentUser(true);
            int received = likeRepo.findByUserTo(currentUser).size();
            int sent = likeRepo.findByUserFrom(currentUser).size();
            int mutual = (int) likeRepo.findByUserFrom(currentUser).stream()
                    .map(UserLike::getUserTo)
                    .filter(u -> u != null && isMutualLike(currentUser, u))
                    .count();

            return ResponseEntity.ok(Map.of(
                    "receivedCount", received,
                    "sentCount", sent,
                    "mutualCount", mutual
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private List<User> deduplicate(List<User> users) {
        Map<Long, User> deduped = new LinkedHashMap<>();
        for (User user : users) {
            if (user != null && user.getId() != null) {
                deduped.putIfAbsent(user.getId(), user);
            }
        }
        return new ArrayList<>(deduped.values());
    }

    private List<UserDto> toDtos(List<User> users, User currentUser, int limit)
            throws AlovoaException, java.security.InvalidKeyException, javax.crypto.IllegalBlockSizeException,
            javax.crypto.BadPaddingException, java.security.NoSuchAlgorithmException, javax.crypto.NoSuchPaddingException,
            java.security.InvalidAlgorithmParameterException, java.io.UnsupportedEncodingException {
        return users.stream()
                .limit(Math.max(limit, 0))
                .map(u -> {
                    try {
                        return UserDto.userToUserDto(UserDto.DtoBuilder.builder()
                                .currentUser(currentUser)
                                .user(u)
                                .userService(userService)
                                .ignoreIntention(ignoreIntention)
                                .build());
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    private boolean isMutualLike(User currentUser, User otherUser) {
        return likeRepo.findByUserFromAndUserTo(currentUser, otherUser) != null
                && likeRepo.findByUserFromAndUserTo(otherUser, currentUser) != null;
    }
}
