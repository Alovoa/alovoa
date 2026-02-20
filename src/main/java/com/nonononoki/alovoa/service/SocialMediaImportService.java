package com.nonononoki.alovoa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserSocialAccount;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.SocialMediaImageImportDto;
import com.nonononoki.alovoa.repo.UserSocialAccountRepository;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SocialMediaImportService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SocialMediaImportService.class);

    private static final String PROVIDER_INSTAGRAM = "instagram";
    private static final String PROVIDER_TIKTOK = "tiktok";
    private static final String PROVIDER_YOUTUBE = "youtube";
    private static final String PROVIDER_X = "x";
    private static final Set<String> X_RESERVED_PATHS = Set.of(
            "home", "i", "intent", "search", "explore", "messages", "settings", "share"
    );
    private static final Set<String> IG_NON_USER_PATHS = Set.of(
            "p", "reel", "reels", "tv", "stories", "explore", "accounts"
    );
    private static final Pattern TIKTOK_HANDLE_PATTERN = Pattern.compile("(?i)/@([^/?#]+)");

    private static final Map<String, ProviderConfig> PROVIDERS = initProviders();

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserSocialAccountRepository userSocialAccountRepo;

    @Value("${app.social-import.enabled:true}")
    private boolean socialImportEnabled;

    @Value("${app.social-import.max-download-bytes:${app.image.max-size:5000000}}")
    private int maxDownloadBytes;

    @Value("${app.social-import.require-linked-ownership:true}")
    private boolean requireLinkedOwnership;

    public SocialMediaImportService(RestTemplate restTemplate,
                                    ObjectMapper objectMapper,
                                    UserSocialAccountRepository userSocialAccountRepo) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userSocialAccountRepo = userSocialAccountRepo;
    }

    public ImportedImage importImage(User user, SocialMediaImageImportDto dto) throws AlovoaException {
        if (!socialImportEnabled) {
            throw new AlovoaException("social_media_import_disabled");
        }
        if (dto == null) {
            throw new AlovoaException("social_media_import_invalid_request");
        }

        String postUrl = trimToNull(dto.getPostUrl());
        String mediaUrl = trimToNull(dto.getMediaUrl());
        if (postUrl == null && mediaUrl == null) {
            throw new AlovoaException("social_media_import_invalid_request");
        }

        ProviderConfig provider = resolveProvider(dto.getProvider(), postUrl, mediaUrl);

        String resolvedMediaUrl = mediaUrl;
        Set<String> ownerSignals = new LinkedHashSet<>();
        ownerSignals.addAll(extractOwnerSignalsFromUrl(provider.getId(), postUrl));
        ownerSignals.addAll(extractOwnerSignalsFromUrl(provider.getId(), mediaUrl));

        if (resolvedMediaUrl == null) {
            ResolvedPost resolvedPost = resolveMediaUrlFromPost(provider, postUrl);
            resolvedMediaUrl = resolvedPost.mediaUrl;
            ownerSignals.addAll(resolvedPost.ownerSignals);
        }

        URI mediaUri = parseHttpsUri(resolvedMediaUrl, "social_media_import_invalid_media_url");
        validateHost(mediaUri.getHost(), provider.getAllowedMediaHosts(), "social_media_import_invalid_media_url");

        boolean ownershipVerified = false;
        if (requireLinkedOwnership) {
            ownershipVerified = verifyLinkedOwnership(user, provider.getId(), ownerSignals);
            if (!ownershipVerified) {
                throw new AlovoaException("social_media_import_ownership_unverified");
            }
        } else {
            ownershipVerified = tryVerifyLinkedOwnership(user, provider.getId(), ownerSignals);
        }

        DownloadedImage downloadedImage = downloadImage(mediaUri.toString());
        return new ImportedImage(
                provider.getId(),
                mediaUri.toString(),
                downloadedImage.bytes,
                downloadedImage.mimeType,
                ownershipVerified
        );
    }

    private ProviderConfig resolveProvider(String providerInput, String postUrl, String mediaUrl) throws AlovoaException {
        if (StringUtils.isNotBlank(providerInput)) {
            String normalizedProvider = providerInput.trim().toLowerCase(Locale.ROOT);
            ProviderConfig provider = PROVIDERS.get(normalizedProvider);
            if (provider == null) {
                throw new AlovoaException("social_media_import_provider_unsupported");
            }
            validateInputUrlsAgainstProvider(provider, postUrl, mediaUrl);
            return provider;
        }

        String url = postUrl != null ? postUrl : mediaUrl;
        URI uri = parseHttpsUri(url, "social_media_import_invalid_url");
        String host = normalizeHost(uri.getHost());

        for (ProviderConfig provider : PROVIDERS.values()) {
            if (provider.matchesAnyHost(host)) {
                validateInputUrlsAgainstProvider(provider, postUrl, mediaUrl);
                return provider;
            }
        }

        throw new AlovoaException("social_media_import_provider_unsupported");
    }

    private void validateInputUrlsAgainstProvider(ProviderConfig provider, String postUrl, String mediaUrl)
            throws AlovoaException {
        if (postUrl != null) {
            URI postUri = parseHttpsUri(postUrl, "social_media_import_invalid_post_url");
            validateHost(postUri.getHost(), provider.getAllowedPostHosts(), "social_media_import_invalid_post_url");
        }
        if (mediaUrl != null) {
            URI mediaUri = parseHttpsUri(mediaUrl, "social_media_import_invalid_media_url");
            validateHost(mediaUri.getHost(), provider.getAllowedMediaHosts(), "social_media_import_invalid_media_url");
        }
    }

    private ResolvedPost resolveMediaUrlFromPost(ProviderConfig provider, String postUrl) throws AlovoaException {
        if (postUrl == null) {
            throw new AlovoaException("social_media_import_invalid_post_url");
        }
        if (provider.getOEmbedUrlTemplate() == null) {
            throw new AlovoaException("social_media_import_media_url_required");
        }

        String encodedPostUrl = UriUtils.encode(postUrl, StandardCharsets.UTF_8);
        URI oEmbedUri = URI.create(provider.getOEmbedUrlTemplate().formatted(encodedPostUrl));

        try {
            String responseBody = restTemplate.getForObject(oEmbedUri, String.class);
            if (StringUtils.isBlank(responseBody)) {
                throw new AlovoaException("social_media_import_no_media_found");
            }
            Map<String, Object> payload = objectMapper.readValue(responseBody, new TypeReference<>() {
            });

            String thumbnailUrl = asString(payload.get("thumbnail_url"));
            if (thumbnailUrl == null) {
                thumbnailUrl = asString(payload.get("url"));
            }
            if (thumbnailUrl == null) {
                throw new AlovoaException("social_media_import_no_media_found");
            }

            Set<String> ownerSignals = new LinkedHashSet<>();
            ownerSignals.addAll(extractOwnerSignalsFromUrl(provider.getId(), asString(payload.get("author_url"))));
            String authorName = asString(payload.get("author_name"));
            if (authorName != null) {
                ownerSignals.add(normalizeIdentitySignal(authorName));
            }
            return new ResolvedPost(thumbnailUrl, ownerSignals);
        } catch (RestClientException e) {
            LOGGER.warn("Failed to resolve social oEmbed for provider {} and url {}", provider.getId(), postUrl, e);
            throw new AlovoaException("social_media_import_oembed_failed");
        } catch (IOException e) {
            LOGGER.warn("Failed to parse social oEmbed payload for provider {}", provider.getId(), e);
            throw new AlovoaException("social_media_import_oembed_failed");
        }
    }

    private DownloadedImage downloadImage(String mediaUrl) throws AlovoaException {
        try {
            DownloadedImage image = restTemplate.execute(mediaUrl, HttpMethod.GET, request -> {
                request.getHeaders().setAccept(List.of(
                        MediaType.IMAGE_JPEG,
                        MediaType.IMAGE_PNG,
                        MediaType.valueOf("image/webp"),
                        MediaType.valueOf("image/heic"),
                        MediaType.ALL
                ));
            }, this::extractDownloadedImage);

            if (image == null) {
                throw new AlovoaException("social_media_import_download_failed");
            }
            return image;
        } catch (SocialImportRuntimeException e) {
            throw new AlovoaException(e.getMessage());
        } catch (RestClientException e) {
            LOGGER.warn("Failed to download social media image {}", mediaUrl, e);
            throw new AlovoaException("social_media_import_download_failed");
        }
    }

    private DownloadedImage extractDownloadedImage(ClientHttpResponse response) throws IOException {
        long contentLength = response.getHeaders().getContentLength();
        if (contentLength > maxDownloadBytes) {
            throw new SocialImportRuntimeException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
        }

        String mimeType = response.getHeaders().getContentType() != null
                ? response.getHeaders().getContentType().toString()
                : null;
        if (mimeType == null || !mimeType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new SocialImportRuntimeException("social_media_import_invalid_mime");
        }

        byte[] bytes;
        try (InputStream inputStream = response.getBody();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                throw new SocialImportRuntimeException("social_media_import_download_failed");
            }
            byte[] buffer = new byte[8192];
            int read;
            int total = 0;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > maxDownloadBytes) {
                    throw new SocialImportRuntimeException(AlovoaException.MAX_MEDIA_SIZE_EXCEEDED);
                }
                outputStream.write(buffer, 0, read);
            }
            bytes = outputStream.toByteArray();
        }

        if (bytes.length == 0) {
            throw new SocialImportRuntimeException("social_media_import_download_failed");
        }

        return new DownloadedImage(bytes, mimeType);
    }

    private URI parseHttpsUri(String url, String errorMessage) throws AlovoaException {
        try {
            URI uri = URI.create(url.trim());
            if (uri.getScheme() == null || !uri.getScheme().equalsIgnoreCase("https")) {
                throw new AlovoaException(errorMessage);
            }
            if (StringUtils.isBlank(uri.getHost())) {
                throw new AlovoaException(errorMessage);
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new AlovoaException(errorMessage);
        }
    }

    private void validateHost(String host, Set<String> allowedHosts, String errorMessage) throws AlovoaException {
        String normalizedHost = normalizeHost(host);
        for (String allowed : allowedHosts) {
            String normalizedAllowed = allowed.toLowerCase(Locale.ROOT);
            if (normalizedHost.equals(normalizedAllowed) || normalizedHost.endsWith("." + normalizedAllowed)) {
                return;
            }
        }
        throw new AlovoaException(errorMessage);
    }

    private static String normalizeHost(String host) {
        return host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private static String asString(Object value) {
        if (value instanceof String stringValue && StringUtils.isNotBlank(stringValue)) {
            return stringValue.trim();
        }
        return null;
    }

    private boolean verifyLinkedOwnership(User user, String provider, Set<String> ownerSignals) throws AlovoaException {
        if (user == null) {
            throw new AlovoaException("social_media_import_user_required");
        }

        Optional<UserSocialAccount> linkedOpt = userSocialAccountRepo.findByUserAndProvider(user, provider);
        if (linkedOpt.isEmpty()) {
            throw new AlovoaException("social_media_import_requires_linked_account");
        }

        if (ownerSignals.isEmpty()) {
            return false;
        }

        Set<String> linkedSignals = extractLinkedSignals(provider, linkedOpt.get());
        if (linkedSignals.isEmpty()) {
            return false;
        }

        for (String ownerSignal : ownerSignals) {
            if (linkedSignals.contains(ownerSignal)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryVerifyLinkedOwnership(User user, String provider, Set<String> ownerSignals) {
        if (user == null || ownerSignals == null || ownerSignals.isEmpty()) {
            return false;
        }
        Optional<UserSocialAccount> linkedOpt = userSocialAccountRepo.findByUserAndProvider(user, provider);
        if (linkedOpt.isEmpty()) {
            return false;
        }
        Set<String> linkedSignals = extractLinkedSignals(provider, linkedOpt.get());
        if (linkedSignals.isEmpty()) {
            return false;
        }
        for (String ownerSignal : ownerSignals) {
            if (linkedSignals.contains(ownerSignal)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> extractLinkedSignals(String provider, UserSocialAccount account) {
        Set<String> signals = new LinkedHashSet<>();
        if (StringUtils.isNotBlank(account.getProviderUserId())) {
            signals.add(normalizeIdentitySignal(account.getProviderUserId()));
        }
        if (StringUtils.isNotBlank(account.getProviderUsername())) {
            signals.add(normalizeIdentitySignal(account.getProviderUsername()));
        }
        if (StringUtils.isNotBlank(account.getProfileUrl())) {
            signals.addAll(extractOwnerSignalsFromUrl(provider, account.getProfileUrl()));
        }
        return signals;
    }

    private Set<String> extractOwnerSignalsFromUrl(String provider, String url) {
        Set<String> signals = new LinkedHashSet<>();
        String normalizedProvider = provider == null ? "" : provider.toLowerCase(Locale.ROOT);
        String rawUrl = StringUtils.trimToNull(url);
        if (rawUrl == null) {
            return signals;
        }

        URI uri;
        try {
            uri = URI.create(rawUrl);
        } catch (Exception e) {
            return signals;
        }

        String path = StringUtils.defaultString(uri.getPath(), "");
        List<String> segments = List.of(path.split("/")).stream()
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();

        switch (normalizedProvider) {
            case PROVIDER_TIKTOK -> {
                Matcher matcher = TIKTOK_HANDLE_PATTERN.matcher(path);
                if (matcher.find()) {
                    signals.add(normalizeIdentitySignal(matcher.group(1)));
                }
            }
            case PROVIDER_X -> {
                if (!segments.isEmpty()) {
                    String maybeUser = normalizeIdentitySignal(segments.get(0));
                    if (!X_RESERVED_PATHS.contains(maybeUser)) {
                        signals.add(maybeUser);
                    }
                }
            }
            case PROVIDER_YOUTUBE -> {
                if (!segments.isEmpty()) {
                    String first = segments.get(0);
                    if (first.startsWith("@")) {
                        signals.add(normalizeIdentitySignal(first));
                    } else if ((first.equalsIgnoreCase("channel")
                            || first.equalsIgnoreCase("user")
                            || first.equalsIgnoreCase("c")) && segments.size() > 1) {
                        signals.add(normalizeIdentitySignal(segments.get(1)));
                    }
                }
            }
            case PROVIDER_INSTAGRAM -> {
                if (!segments.isEmpty()) {
                    String maybeUser = normalizeIdentitySignal(segments.get(0));
                    if (!IG_NON_USER_PATHS.contains(maybeUser)) {
                        signals.add(maybeUser);
                    }
                }
            }
            default -> {
            }
        }
        return signals;
    }

    private String normalizeIdentitySignal(String value) {
        String normalized = StringUtils.trimToEmpty(value).toLowerCase(Locale.ROOT);
        normalized = URLDecoder.decode(normalized, StandardCharsets.UTF_8);
        if (normalized.startsWith("@")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("https://")) {
            normalized = normalized.substring(8);
        } else if (normalized.startsWith("http://")) {
            normalized = normalized.substring(7);
        }
        normalized = normalized.replaceAll("^www\\.", "");
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.contains("/")) {
            String[] parts = normalized.split("/");
            normalized = parts[parts.length - 1];
        }
        return normalized.trim();
    }

    private static Map<String, ProviderConfig> initProviders() {
        Map<String, ProviderConfig> providers = new LinkedHashMap<>();

        providers.put(PROVIDER_INSTAGRAM, new ProviderConfig(
                PROVIDER_INSTAGRAM,
                Set.of("instagram.com"),
                Set.of("cdninstagram.com"),
                null
        ));

        providers.put(PROVIDER_TIKTOK, new ProviderConfig(
                PROVIDER_TIKTOK,
                Set.of("tiktok.com"),
                Set.of("tiktokcdn.com"),
                "https://www.tiktok.com/oembed?url=%s"
        ));

        providers.put(PROVIDER_YOUTUBE, new ProviderConfig(
                PROVIDER_YOUTUBE,
                Set.of("youtube.com", "youtu.be"),
                Set.of("ytimg.com"),
                "https://www.youtube.com/oembed?url=%s&format=json"
        ));

        providers.put(PROVIDER_X, new ProviderConfig(
                PROVIDER_X,
                Set.of("x.com", "twitter.com"),
                Set.of("twimg.com"),
                null
        ));

        return providers;
    }

    @Getter
    public static class ImportedImage {
        private final String provider;
        private final String sourceUrl;
        private final byte[] bytes;
        private final String mimeType;
        private final boolean sourceVerified;

        public ImportedImage(String provider, String sourceUrl, byte[] bytes, String mimeType, boolean sourceVerified) {
            this.provider = provider;
            this.sourceUrl = sourceUrl;
            this.bytes = bytes;
            this.mimeType = mimeType;
            this.sourceVerified = sourceVerified;
        }
    }

    private static class ProviderConfig {
        @Getter
        private final String id;
        @Getter
        private final Set<String> allowedPostHosts;
        @Getter
        private final Set<String> allowedMediaHosts;
        @Getter
        private final String oEmbedUrlTemplate;

        ProviderConfig(String id, Set<String> allowedPostHosts, Set<String> allowedMediaHosts, String oEmbedUrlTemplate) {
            this.id = id;
            this.allowedPostHosts = allowedPostHosts;
            this.allowedMediaHosts = allowedMediaHosts;
            this.oEmbedUrlTemplate = oEmbedUrlTemplate;
        }

        boolean matchesAnyHost(String host) {
            for (String allowedHost : allowedPostHosts) {
                if (host.equals(allowedHost) || host.endsWith("." + allowedHost)) {
                    return true;
                }
            }
            for (String allowedHost : allowedMediaHosts) {
                if (host.equals(allowedHost) || host.endsWith("." + allowedHost)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class DownloadedImage {
        private final byte[] bytes;
        private final String mimeType;

        private DownloadedImage(byte[] bytes, String mimeType) {
            this.bytes = bytes;
            this.mimeType = mimeType;
        }
    }

    private static class SocialImportRuntimeException extends RuntimeException {
        private SocialImportRuntimeException(String message) {
            super(message);
        }
    }

    private static class ResolvedPost {
        private final String mediaUrl;
        private final Set<String> ownerSignals;

        private ResolvedPost(String mediaUrl, Set<String> ownerSignals) {
            this.mediaUrl = mediaUrl;
            this.ownerSignals = ownerSignals == null ? Set.of() : ownerSignals;
        }
    }
}
