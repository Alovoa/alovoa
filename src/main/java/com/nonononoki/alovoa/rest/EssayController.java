package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.EssayDto;
import com.nonononoki.alovoa.service.EssayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for managing user essays (the 10 fixed OKCupid-style profile prompts).
 */
@RestController
@RequestMapping("/api/v1/essays")
public class EssayController {

    @Autowired
    private EssayService essayService;

    /**
     * Get all essays for the current user with templates.
     */
    @GetMapping
    public ResponseEntity<List<EssayDto>> getEssays() throws AlovoaException {
        return ResponseEntity.ok(essayService.getCurrentUserEssays());
    }

    /**
     * Mobile compatibility alias.
     */
    @GetMapping("/list")
    public ResponseEntity<List<EssayDto>> getEssaysList() throws AlovoaException {
        return getEssays();
    }

    /**
     * Get essay templates (prompts only, no user answers).
     */
    @GetMapping("/templates")
    public ResponseEntity<List<EssayDto>> getTemplates() throws AlovoaException {
        return ResponseEntity.ok(essayService.getEssayTemplates());
    }

    /**
     * Mobile compatibility alias.
     */
    @GetMapping("/prompts")
    public ResponseEntity<List<EssayDto>> getPrompts() throws AlovoaException {
        return getTemplates();
    }

    /**
     * Get count of filled essays for the current user.
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getFilledCount() throws AlovoaException {
        return ResponseEntity.ok(Map.of("count", essayService.getFilledEssayCount()));
    }

    /**
     * Save a single essay.
     */
    @PostMapping("/{promptId}")
    public ResponseEntity<Void> saveEssay(
            @PathVariable Long promptId,
            @RequestBody Map<String, String> body) throws AlovoaException {
        String text = body.get("text");
        essayService.saveEssay(promptId, text);
        return ResponseEntity.ok().build();
    }

    /**
     * Mobile compatibility alias.
     * Accepts either:
     * { "promptId": 1, "text": "..." } or { "1": "..." }.
     */
    @PostMapping("/add")
    public ResponseEntity<?> addEssay(@RequestBody Map<String, Object> body) throws AlovoaException {
        Object promptIdRaw = body.get("promptId");
        if (promptIdRaw != null) {
            Long promptId = promptIdRaw instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(promptIdRaw));
            String text = body.get("text") != null ? String.valueOf(body.get("text")) : null;
            essayService.saveEssay(promptId, text);
            return ResponseEntity.ok().build();
        }

        Map<Long, String> essayMap = body.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> Long.parseLong(e.getKey()),
                        e -> e.getValue() != null ? String.valueOf(e.getValue()) : null
                ));
        essayService.saveEssays(essayMap);
        return ResponseEntity.ok().build();
    }

    /**
     * Save multiple essays at once.
     * Request body: { "1": "My self summary...", "2": "I work as...", ... }
     */
    @PostMapping
    public ResponseEntity<Void> saveEssays(@RequestBody Map<String, String> essays) throws AlovoaException {
        // Convert string keys to Long
        Map<Long, String> essayMap = essays.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        e -> Long.parseLong(e.getKey()),
                        Map.Entry::getValue
                ));
        essayService.saveEssays(essayMap);
        return ResponseEntity.ok().build();
    }

    /**
     * Mobile compatibility alias.
     */
    @PostMapping("/update/{promptId}")
    public ResponseEntity<Void> updateEssay(
            @PathVariable Long promptId,
            @RequestBody Map<String, String> body) throws AlovoaException {
        return saveEssay(promptId, body);
    }

    /**
     * Delete an essay (clear the answer).
     */
    @DeleteMapping("/{promptId}")
    public ResponseEntity<Void> deleteEssay(@PathVariable Long promptId) throws AlovoaException {
        essayService.saveEssay(promptId, null);
        return ResponseEntity.ok().build();
    }

    /**
     * Mobile compatibility alias.
     */
    @DeleteMapping("/delete/{promptId}")
    public ResponseEntity<Void> deleteEssayAlias(@PathVariable Long promptId) throws AlovoaException {
        return deleteEssay(promptId);
    }
}
