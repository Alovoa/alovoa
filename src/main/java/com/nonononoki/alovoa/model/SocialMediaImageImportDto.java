package com.nonononoki.alovoa.model;

import lombok.Data;

@Data
public class SocialMediaImageImportDto {

    /**
     * Optional provider hint. If omitted, provider will be inferred from URL host.
     * Supported values: instagram, tiktok, youtube, x
     */
    private String provider;

    /**
     * Social post URL (for providers that support oEmbed-based thumbnail extraction).
     */
    private String postUrl;

    /**
     * Direct media URL from a validated social media CDN/domain.
     */
    private String mediaUrl;
}
