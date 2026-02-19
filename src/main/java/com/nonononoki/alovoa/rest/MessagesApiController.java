package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Conversation;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.MessageDto;
import com.nonononoki.alovoa.model.MessageReactionDto;
import com.nonononoki.alovoa.repo.ConversationRepository;
import com.nonononoki.alovoa.service.AuthService;
import com.nonononoki.alovoa.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API-first messages controller.
 */
@RestController
@RequestMapping("/api/v1/messages")
public class MessagesApiController {

    private static final int MAX_MESSAGES = 50;

    @Autowired
    private AuthService authService;

    @Autowired
    private ConversationRepository conversationRepo;

    @Autowired
    private MessageService messageService;

    /**
     * Open messaging endpoint (OKCupid 2016 parity).
     * Creates a conversation if needed and can send an initial message immediately.
     */
    @PostMapping("/open/{targetUserUuid}")
    public ResponseEntity<?> openConversation(
            @PathVariable UUID targetUserUuid,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String initialMessage = body != null ? body.get("message") : null;
            Conversation conversation = messageService.openConversation(targetUserUuid, initialMessage);
            User currentUser = authService.getCurrentUser(true);
            User partner = conversation.getPartner(currentUser);

            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversation.getId());
            response.put("partnerUuid", partner.getUuid());
            response.put("partnerName", partner.getFirstName());
            response.put("lastUpdated", conversation.getLastUpdated());
            response.put("initialMessageSent", initialMessage != null && !initialMessage.trim().isEmpty());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Paginated JSON endpoint for conversation messages.
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<?> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "1") int page) {
        try {
            User user = authService.getCurrentUser(true);
            Conversation conversation = conversationRepo.findById(conversationId).orElse(null);

            if (conversation == null) {
                throw new AlovoaException("conversation_not_found");
            }
            if (!conversation.containsUser(user)) {
                throw new AlovoaException("user_not_in_conversation");
            }

            User partner = conversation.getPartner(user);
            if (isBlocked(user, partner) || isBlocked(partner, user)) {
                throw new AlovoaException("user_blocked");
            }

            List<MessageDto> allMessages = MessageDto.messagesToDtos(conversation.getMessages(), user);
            int totalMessages = allMessages.size();
            int pageSize = MAX_MESSAGES;
            int safePage = Math.max(page, 1);
            int start = Math.max(totalMessages - (safePage * pageSize), 0);
            int end = Math.max(totalMessages - ((safePage - 1) * pageSize), 0);

            List<MessageDto> pageMessages = allMessages.subList(start, end);
            boolean hasMore = start > 0;

            Map<String, Object> response = new HashMap<>();
            response.put("conversationId", conversationId);
            response.put("page", safePage);
            response.put("hasMore", hasMore);
            response.put("totalMessages", totalMessages);
            response.put("messages", pageMessages);
            response.put("currentUserId", user.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Reaction list endpoint for a message.
     */
    @GetMapping("/{messageId}/reactions")
    public ResponseEntity<?> getMessageReactions(@PathVariable Long messageId) {
        try {
            return ResponseEntity.ok(messageService.getMessageReactions(messageId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Convenience alias for sending text from API-first clients.
     */
    @PostMapping("/send/{conversationId}")
    public ResponseEntity<?> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> body) {
        try {
            String message = body != null ? body.get("message") : null;
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "message_required"));
            }
            messageService.send(conversationId, message.trim());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (AlovoaException | GeneralSecurityException | java.io.IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isBlocked(User from, User to) {
        return from.getBlockedUsers().stream()
                .filter(o -> o.getUserTo() != null)
                .anyMatch(o -> o.getUserTo().getId().equals(to.getId()));
    }
}
