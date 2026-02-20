package com.nonononoki.alovoa.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SocialLinkedAccountDto {
    private UUID id;
    private String provider;
    private String providerUserId;
    private String providerUsername;
    private String profileUrl;
    private Date linkedAt;
}
