package com.nonononoki.alovoa.service.social;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialProviderProfile {
    private String providerUserId;
    private String providerUsername;
    private String profileUrl;
}
