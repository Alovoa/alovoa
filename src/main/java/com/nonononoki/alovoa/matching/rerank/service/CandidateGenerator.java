package com.nonononoki.alovoa.matching.rerank.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.matching.rerank.model.MatchingRequestContext;
import com.nonononoki.alovoa.matching.rerank.model.RerankCandidate;
import com.nonononoki.alovoa.model.MatchRecommendationDto;

import java.util.List;

/**
 * Adapter contract that keeps existing candidate generation untouched.
 * The current pipeline can continue producing candidate DTOs; reranker consumes via this interface.
 */
public interface CandidateGenerator {

    @SuppressWarnings("unused")
    List<RerankCandidate> getCandidates(User viewer,
                                        List<MatchRecommendationDto> existingCandidates,
                                        MatchingRequestContext context);
}
