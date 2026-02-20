package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.SocialLinkSessionStatusDto;
import com.nonononoki.alovoa.model.SocialLinkStartResponseDto;
import com.nonononoki.alovoa.model.SocialLinkedAccountDto;
import com.nonononoki.alovoa.service.SocialConnectService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.List;
import java.util.UUID;

@RestController
public class SocialConnectController {

    private final SocialConnectService socialConnectService;

    public SocialConnectController(SocialConnectService socialConnectService) {
        this.socialConnectService = socialConnectService;
    }

    @PostMapping("/api/v1/social-connect/start/{provider}")
    public ResponseEntity<SocialLinkStartResponseDto> start(@PathVariable String provider) throws AlovoaException {
        return ResponseEntity.ok(socialConnectService.startLink(provider));
    }

    @GetMapping("/api/v1/social-connect/session/{sessionId}")
    public ResponseEntity<SocialLinkSessionStatusDto> session(@PathVariable UUID sessionId) throws AlovoaException {
        return ResponseEntity.ok(socialConnectService.getLinkSessionStatus(sessionId));
    }

    @GetMapping("/api/v1/social-connect/accounts")
    public ResponseEntity<List<SocialLinkedAccountDto>> listAccounts() throws AlovoaException {
        return ResponseEntity.ok(socialConnectService.listLinkedAccounts());
    }

    @DeleteMapping("/api/v1/social-connect/accounts/{provider}")
    public void unlink(@PathVariable String provider) throws AlovoaException {
        socialConnectService.unlinkAccount(provider);
    }

    @GetMapping(value = "/oauth2/connect/{provider}/callback", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> callback(@PathVariable String provider,
                                           @RequestParam(required = false) String state,
                                           @RequestParam(required = false) String code,
                                           @RequestParam(required = false) String error,
                                           @RequestParam(name = "error_description", required = false) String errorDescription) {
        try {
            SocialLinkSessionStatusDto status = socialConnectService.completeLinkCallback(
                    provider, state, code, error, errorDescription
            );
            return ResponseEntity.ok(buildCallbackHtml(status));
        } catch (AlovoaException e) {
            SocialLinkSessionStatusDto fallback = new SocialLinkSessionStatusDto(
                    null,
                    provider,
                    "FAILED",
                    e.getMessage(),
                    null,
                    null,
                    null
            );
            return ResponseEntity.ok(buildCallbackHtml(fallback));
        }
    }

    private String buildCallbackHtml(SocialLinkSessionStatusDto status) {
        boolean success = "LINKED".equalsIgnoreCase(status.getStatus());
        String title = success ? "Social account connected" : "Social connection failed";
        String detail = success
                ? "You can return to ALOVOA now."
                : HtmlUtils.htmlEscape(status.getErrorMessage() == null ? "Unknown error" : status.getErrorMessage());

        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1" />
                  <title>%s</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; margin: 0; background:#f6f7fb; color:#1f2937; }
                    .wrap { max-width: 560px; margin: 10vh auto; background:#fff; border-radius: 14px; box-shadow: 0 12px 32px rgba(0,0,0,0.08); padding: 24px; }
                    h1 { margin-top: 0; font-size: 1.25rem; }
                    p { line-height: 1.45; }
                    .status { font-weight: 700; color: %s; }
                  </style>
                </head>
                <body>
                  <div class="wrap">
                    <h1>%s</h1>
                    <p class="status">%s</p>
                    <p>%s</p>
                    <p>You may close this tab.</p>
                  </div>
                </body>
                </html>
                """.formatted(
                HtmlUtils.htmlEscape(title),
                success ? "#0f9d58" : "#d93025",
                HtmlUtils.htmlEscape(title),
                success ? "Connected" : "Failed",
                detail
        );
    }
}
