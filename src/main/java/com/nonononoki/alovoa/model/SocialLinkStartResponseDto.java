package com.nonononoki.alovoa.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialLinkStartResponseDto {
    private UUID sessionId;
    private String provider;
    private String authorizationUrl;
    private Date expiresAt;
}
