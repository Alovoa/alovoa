package com.nonononoki.alovoa.matching.rerank;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.entity.user.UserDates;
import com.nonononoki.alovoa.matching.rerank.model.RerankResult;
import com.nonononoki.alovoa.matching.rerank.model.RerankerConfig;
import com.nonononoki.alovoa.matching.rerank.model.UserStatsSnapshot;
import com.nonononoki.alovoa.matching.rerank.policy.impl.DefaultCapacityPolicy;
import com.nonononoki.alovoa.matching.rerank.policy.impl.DefaultCollaborativePriorPolicy;
import com.nonononoki.alovoa.matching.rerank.policy.impl.DefaultDesirabilityPolicy;
import com.nonononoki.alovoa.matching.rerank.policy.impl.DefaultExplorationPolicy;
import com.nonononoki.alovoa.matching.rerank.policy.impl.DefaultExposurePolicy;
import com.nonononoki.alovoa.matching.rerank.service.*;
import com.nonononoki.alovoa.model.MatchRecommendationDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MatchRerankerServiceTest {

    @Test
    void deterministicSortingAndHardFloor() {
        CandidateGenerator candidateGenerator = new ExistingPipelineCandidateGenerator();
        SegmentKeyService segmentKeyService = new SegmentKeyService();

        RerankerPolicyConfigService cfgService = Mockito.mock(RerankerPolicyConfigService.class);
        RollingStatsReadService statsReadService = Mockito.mock(RollingStatsReadService.class);

        RerankerConfig cfg = new RerankerConfig();
        cfg.setSMin(0.80);
        cfg.setEpsilon(0.0);

        RerankerPolicyConfigService.ResolvedConfig resolved = RerankerPolicyConfigService.ResolvedConfig.builder()
                .enabled(true)
                .segmentKey("seg")
                .source("test")
                .config(cfg)
                .build();

        Mockito.when(cfgService.resolve(Mockito.anyString())).thenReturn(resolved);
        Mockito.when(cfgService.assignVariant(Mockito.anyLong(), Mockito.eq(resolved))).thenReturn("treatment");

        Mockito.when(statsReadService.getSnapshot(Mockito.anyLong(), Mockito.anyString()))
                .thenReturn(UserStatsSnapshot.empty(99L, "seg"));
        Mockito.when(statsReadService.getSnapshots(Mockito.anyCollection(), Mockito.anyString()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Collection<Long> ids = (Collection<Long>) invocation.getArguments()[0];
                    Map<Long, UserStatsSnapshot> out = new HashMap<>();
                    for (Long id : ids) {
                        out.put(id, UserStatsSnapshot.empty(id, "seg"));
                    }
                    return out;
                });
        Mockito.when(statsReadService.getViewerBucketImpressions(Mockito.anyLong())).thenReturn(Map.of());

        MatchRerankerService service = new MatchRerankerService(
                candidateGenerator,
                segmentKeyService,
                cfgService,
                statsReadService,
                new DefaultExposurePolicy(),
                new DefaultCapacityPolicy(),
                new DefaultDesirabilityPolicy(),
                new DefaultExplorationPolicy(),
                new DefaultCollaborativePriorPolicy()
        );

        User viewer = viewer(100L);

        MatchRecommendationDto c1 = candidate(3L, 80.0);
        MatchRecommendationDto c2 = candidate(1L, 80.0);
        MatchRecommendationDto c3 = candidate(2L, 79.0); // below s_min

        RerankResult result = service.rerank(viewer, List.of(c1, c2, c3), "req-1", "daily_matches");

        assertTrue(result.isRerankerApplied());
        assertEquals(2, result.getRanked().size());
        // tie broken deterministically by original position from existing pipeline
        assertEquals(3L, result.getRanked().get(0).getUserId());
        assertEquals(1L, result.getRanked().get(1).getUserId());
    }

    @Test
    void policyFailureFallsBackToExistingOrder() {
        CandidateGenerator candidateGenerator = new ExistingPipelineCandidateGenerator();
        SegmentKeyService segmentKeyService = new SegmentKeyService();

        RerankerPolicyConfigService cfgService = Mockito.mock(RerankerPolicyConfigService.class);
        RollingStatsReadService statsReadService = Mockito.mock(RollingStatsReadService.class);

        RerankerConfig cfg = new RerankerConfig();
        cfg.setSMin(0.0);

        RerankerPolicyConfigService.ResolvedConfig resolved = RerankerPolicyConfigService.ResolvedConfig.builder()
                .enabled(true)
                .segmentKey("seg")
                .source("test")
                .config(cfg)
                .build();

        Mockito.when(cfgService.resolve(Mockito.anyString())).thenReturn(resolved);
        Mockito.when(cfgService.assignVariant(Mockito.anyLong(), Mockito.eq(resolved))).thenReturn("treatment");

        MatchRerankerService service = new MatchRerankerService(
                candidateGenerator,
                segmentKeyService,
                cfgService,
                statsReadService,
                (candidateStats, config) -> { throw new RuntimeException("boom"); },
                new DefaultCapacityPolicy(),
                new DefaultDesirabilityPolicy(),
                new DefaultExplorationPolicy(),
                new DefaultCollaborativePriorPolicy()
        );

        User viewer = viewer(101L);
        MatchRecommendationDto c1 = candidate(10L, 75.0);
        MatchRecommendationDto c2 = candidate(11L, 65.0);

        List<MatchRecommendationDto> existing = List.of(c1, c2);
        RerankResult result = service.rerank(viewer, existing, "req-2", "daily_matches");

        assertFalse(result.isRerankerApplied());
        assertEquals("reranker_error_fallback", result.getReason());
        assertEquals(existing, result.getRanked());
    }

    private User viewer(Long id) {
        User user = new User("viewer" + id + "@example.com");
        user.setId(id);
        user.setCountry("US");

        Gender g = new Gender();
        g.setText("male");
        user.setGender(g);
        user.setPreferedGenders(new HashSet<>(Set.of(g)));

        UserDates dates = new UserDates();
        dates.setDateOfBirth(Date.from(Instant.parse("1993-01-01T00:00:00Z")));
        user.setDates(dates);
        return user;
    }

    private MatchRecommendationDto candidate(Long id, Double compatibility) {
        MatchRecommendationDto dto = new MatchRecommendationDto();
        dto.setUserId(id);
        dto.setUserUuid(UUID.randomUUID().toString());
        dto.setCompatibilityScore(compatibility);
        return dto;
    }
}
