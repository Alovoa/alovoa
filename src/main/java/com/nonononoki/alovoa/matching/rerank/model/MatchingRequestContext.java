package com.nonononoki.alovoa.matching.rerank.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MatchingRequestContext {
    String requestId;
    String surface;
    String segmentKey;
    String variant;
}
