package com.nonononoki.alovoa.matching.rerank.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.matching.rerank.model.MatchingRequestContext;
import com.nonononoki.alovoa.matching.rerank.model.RerankCandidate;
import com.nonononoki.alovoa.model.MatchRecommendationDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ExistingPipelineCandidateGenerator implements CandidateGenerator {

    @Override
    public List<RerankCandidate> getCandidates(User viewer,
                                               List<MatchRecommendationDto> existingCandidates,
                                               MatchingRequestContext context) {
        List<RerankCandidate> out = new ArrayList<>();
        int position = 0;
        for (MatchRecommendationDto dto : existingCandidates) {
            if (dto == null || dto.getUserId() == null) {
                continue;
            }
            double normalizedBase = normalize(dto.getCompatibilityScore());
            out.add(RerankCandidate.builder()
                    .candidateId(dto.getUserId())
                    .candidateUuid(dto.getUserUuid())
                    .baseScore(normalizedBase)
                    .originalPosition(position++)
                    .payload(dto)
                    .build());
        }
        return out;
    }

    private double normalize(Double compatibilityScore) {
        if (compatibilityScore == null) {
            return 0.0;
        }
        double v = compatibilityScore / 100.0;
        if (v < 0.0) {
            return 0.0;
        }
        return Math.min(1.0, v);
    }
}
