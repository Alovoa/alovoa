package com.nonononoki.alovoa.service.social;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthAccessToken {
    private String accessToken;
    private String refreshToken;
    private Long expiresInSeconds;
    private String tokenType;
}
