package com.nonononoki.alovoa.rest;

import com.nonononoki.alovoa.entity.user.UserProfileVisit;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.service.ProfileVisitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Expo/mobile compatibility controller for visitors APIs.
 */
@RestController
@RequestMapping("/api/v1/visitors")
public class VisitorsController {

    @Autowired
    private ProfileVisitService visitService;

    @GetMapping("/list")
    public ResponseEntity<?> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<UserProfileVisit> visitors = visitService.getMyVisitors(page, size);
            Map<String, Object> response = new HashMap<>();
            response.put("visitors", visitors.getContent().stream()
                    .map(this::mapVisit)
                    .collect(Collectors.toList()));
            response.put("totalCount", visitors.getTotalElements());
            response.put("totalPages", visitors.getTotalPages());
            response.put("currentPage", page);
            return ResponseEntity.ok(response);
        } catch (AlovoaException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/count")
    public ResponseEntity<?> count(@RequestParam(defaultValue = "7") int days) {
        try {
            long total = visitService.getTotalVisitorCount();
            long recent = visitService.getRecentVisitorCount(days);
            return ResponseEntity.ok(Map.of(
                    "totalCount", total,
                    "recentCount", recent,
                    "days", days
            ));
        } catch (AlovoaException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> mapVisit(UserProfileVisit visit) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", visit.getId());
        dto.put("visitedAt", visit.getVisitedAt());
        dto.put("lastVisitAt", visit.getLastVisitAt());
        dto.put("visitCount", visit.getVisitCount());

        if (visit.getVisitor() != null) {
            dto.put("userId", visit.getVisitor().getUuid());
            dto.put("userName", visit.getVisitor().getFirstName());
            dto.put("profilePicture", visit.getVisitor().getProfilePicture() != null
                    ? "/media/profile-picture/" + visit.getVisitor().getProfilePicture().getUuid()
                    : null);
        }
        return dto;
    }
}

