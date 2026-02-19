package com.nonononoki.alovoa.matching.rerank;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.matching.FeatureFlagConfig;
import com.nonononoki.alovoa.matching.rerank.service.RerankerPolicyConfigService;
import com.nonononoki.alovoa.repo.matching.FeatureFlagConfigRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RerankerPolicyConfigServiceTest {

    @Test
    void segmentConfigOverridesGlobal() {
        FeatureFlagConfigRepository repo = Mockito.mock(FeatureFlagConfigRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        FeatureFlagConfig segment = new FeatureFlagConfig();
        segment.setFlagName(RerankerPolicyConfigService.FLAG_NAME);
        segment.setSegmentKey("gender:male|seeking:female|age:25_29|geo:us");
        segment.setEnabled(true);
        segment.setJsonConfig("{\"tau\":150,\"trafficPercent\":10}");

        FeatureFlagConfig global = new FeatureFlagConfig();
        global.setFlagName(RerankerPolicyConfigService.FLAG_NAME);
        global.setSegmentKey("*");
        global.setEnabled(false);
        global.setJsonConfig("{\"tau\":200}");

        Mockito.when(repo.findByFlagNameAndSegmentKey(RerankerPolicyConfigService.FLAG_NAME, segment.getSegmentKey()))
                .thenReturn(Optional.of(segment));
        Mockito.when(repo.findByFlagNameAndSegmentKey(RerankerPolicyConfigService.FLAG_NAME, "*"))
                .thenReturn(Optional.of(global));

        RerankerPolicyConfigService service = new RerankerPolicyConfigService(repo, mapper);
        RerankerPolicyConfigService.ResolvedConfig resolved = service.resolve(segment.getSegmentKey());

        assertTrue(resolved.isEnabled());
        assertEquals(150.0, resolved.getConfig().getTau(), 1e-9);
        assertEquals(10, resolved.getConfig().getTrafficPercent());
    }

    @Test
    void variantAssignmentIsDeterministicAndRespectsTraffic() {
        FeatureFlagConfigRepository repo = Mockito.mock(FeatureFlagConfigRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        FeatureFlagConfig global = new FeatureFlagConfig();
        global.setFlagName(RerankerPolicyConfigService.FLAG_NAME);
        global.setSegmentKey("*");
        global.setEnabled(true);
        global.setJsonConfig("{\"trafficPercent\":10}");

        Mockito.when(repo.findByFlagNameAndSegmentKey(RerankerPolicyConfigService.FLAG_NAME, "segment"))
                .thenReturn(Optional.empty());
        Mockito.when(repo.findByFlagNameAndSegmentKey(RerankerPolicyConfigService.FLAG_NAME, "*"))
                .thenReturn(Optional.of(global));

        RerankerPolicyConfigService service = new RerankerPolicyConfigService(repo, mapper);
        RerankerPolicyConfigService.ResolvedConfig resolved = service.resolve("segment");

        String v1 = service.assignVariant(12345L, resolved);
        String v2 = service.assignVariant(12345L, resolved);
        assertEquals(v1, v2);

        global.setJsonConfig("{\"trafficPercent\":0}");
        resolved = service.resolve("segment");
        assertEquals("control", service.assignVariant(12345L, resolved));
    }
}
