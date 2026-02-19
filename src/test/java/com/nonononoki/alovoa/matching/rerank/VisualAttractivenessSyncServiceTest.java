package com.nonononoki.alovoa.matching.rerank;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.matching.UserVisualAttractiveness;
import com.nonononoki.alovoa.entity.user.UserProfilePicture;
import com.nonononoki.alovoa.matching.rerank.service.VisualAttractivenessSyncService;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.repo.matching.UserVisualAttractivenessRepository;
import com.nonononoki.alovoa.service.S3StorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;

class VisualAttractivenessSyncServiceTest {

    @Test
    void refreshVisualScoresPersistsHiddenScore() {
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        UserVisualAttractivenessRepository visualRepo = Mockito.mock(UserVisualAttractivenessRepository.class);
        S3StorageService s3StorageService = Mockito.mock(S3StorageService.class);
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

        VisualAttractivenessSyncService service = new VisualAttractivenessSyncService(
                userRepo,
                visualRepo,
                s3StorageService,
                restTemplate
        );
        ReflectionTestUtils.setField(service, "mediaServiceUrl", "http://media");
        ReflectionTestUtils.setField(service, "maxUsersPerRun", 10);
        ReflectionTestUtils.setField(service, "rescoreDays", 14);

        User user = buildUser(100L, "profile/u100.webp", "image/webp");

        Mockito.when(userRepo.findByDisabledFalseAndAdminFalseAndConfirmedTrue()).thenReturn(List.of(user));
        Mockito.when(visualRepo.findByUserIdIn(anyCollection())).thenReturn(List.of());
        Mockito.when(s3StorageService.downloadMedia("profile/u100.webp"))
                .thenReturn("bytes".getBytes(StandardCharsets.UTF_8));

        Map<String, Object> response = new HashMap<>();
        response.put("score", 0.82);
        response.put("confidence", 0.66);
        response.put("provider", "deepface+mediapipe");
        response.put("model_version", "oss_v1");
        Mockito.when(restTemplate.exchange(
                eq("http://media/attractiveness/score"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(ResponseEntity.ok(response));

        service.refreshVisualScores();

        ArgumentCaptor<UserVisualAttractiveness> saved = ArgumentCaptor.forClass(UserVisualAttractiveness.class);
        Mockito.verify(visualRepo).save(saved.capture());
        UserVisualAttractiveness row = saved.getValue();

        assertNotNull(row);
        assertEquals(100L, row.getUserId());
        assertEquals(0.82, row.getVisualScore(), 1e-9);
        assertEquals(0.66, row.getConfidence(), 1e-9);
        assertEquals("deepface+mediapipe", row.getSourceProvider());
        assertEquals("oss_v1", row.getModelVersion());
    }

    @Test
    void refreshVisualScoresSkipsFreshRows() {
        UserRepository userRepo = Mockito.mock(UserRepository.class);
        UserVisualAttractivenessRepository visualRepo = Mockito.mock(UserVisualAttractivenessRepository.class);
        S3StorageService s3StorageService = Mockito.mock(S3StorageService.class);
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);

        VisualAttractivenessSyncService service = new VisualAttractivenessSyncService(
                userRepo,
                visualRepo,
                s3StorageService,
                restTemplate
        );
        ReflectionTestUtils.setField(service, "mediaServiceUrl", "http://media");
        ReflectionTestUtils.setField(service, "maxUsersPerRun", 10);
        ReflectionTestUtils.setField(service, "rescoreDays", 14);

        User user = buildUser(101L, "profile/u101.webp", "image/webp");
        UserVisualAttractiveness existing = new UserVisualAttractiveness();
        existing.setUserId(101L);
        existing.setUpdatedAt(Date.from(Instant.now()));

        Mockito.when(userRepo.findByDisabledFalseAndAdminFalseAndConfirmedTrue()).thenReturn(List.of(user));
        Mockito.when(visualRepo.findByUserIdIn(anyCollection())).thenReturn(List.of(existing));

        service.refreshVisualScores();

        Mockito.verifyNoInteractions(s3StorageService);
        Mockito.verifyNoInteractions(restTemplate);
        Mockito.verify(visualRepo, Mockito.never()).save(any(UserVisualAttractiveness.class));
    }

    private User buildUser(Long id, String s3Key, String mime) {
        User user = new User("u" + id + "@example.com");
        user.setId(id);
        user.setConfirmed(true);
        user.setAdmin(false);
        user.setDisabled(false);

        UserProfilePicture picture = new UserProfilePicture();
        picture.setS3Key(s3Key);
        picture.setBinMime(mime);
        user.setProfilePicture(picture);
        return user;
    }
}
