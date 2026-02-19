package com.nonononoki.alovoa.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.UserCalendarSettings;
import com.nonononoki.alovoa.repo.UserCalendarSettingsRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST endpoints used by the Expo match-window/calendar screens.
 */
@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarApiController {

    private static final List<String> DAY_KEYS = List.of(
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    private static final int SLOT_MINUTES = 30;

    @Autowired
    private AuthService authService;

    @Autowired
    private UserCalendarSettingsRepository settingsRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/availability")
    public ResponseEntity<?> getAvailability() {
        try {
            User user = authService.getCurrentUser(true);
            return ResponseEntity.ok(getAvailabilityForUser(user, true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/update")
    public ResponseEntity<?> updateAvailability(@RequestBody Map<String, Object> availability) {
        try {
            User user = authService.getCurrentUser(true);
            UserCalendarSettings settings = getOrCreateSettings(user);
            Map<String, Object> normalized = normalizeAvailability(availability != null ? availability : defaultAvailability());
            settings.setWeeklyAvailabilityJson(objectMapper.writeValueAsString(normalized));
            settingsRepo.save(settings);
            return ResponseEntity.ok(Map.of("success", true, "availability", normalized));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/slots/{partnerId}")
    public ResponseEntity<?> getSlots(
            @PathVariable String partnerId,
            @RequestParam(required = false) String date) {
        try {
            User user = authService.getCurrentUser(true);
            User partner = resolvePartner(partnerId);
            if (partner == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Partner not found"));
            }

            LocalDate day = date != null && !date.isBlank() ? LocalDate.parse(date) : LocalDate.now();
            Map<String, Object> userAvailability = getAvailabilityForUser(user, true);
            Map<String, Object> partnerAvailability = getAvailabilityForUser(partner, true);
            List<Map<String, Object>> slots = buildSlotsForDay(day, userAvailability, partnerAvailability);
            return ResponseEntity.ok(slots);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private User resolvePartner(String partnerId) {
        if (partnerId == null || partnerId.isBlank()) {
            return null;
        }
        try {
            return userRepo.findById(Long.parseLong(partnerId)).orElse(null);
        } catch (NumberFormatException ignored) {
            try {
                return userRepo.findByUuid(UUID.fromString(partnerId));
            } catch (Exception ignoredToo) {
                return null;
            }
        }
    }

    private UserCalendarSettings getOrCreateSettings(User user) {
        return settingsRepo.findByUser(user).orElseGet(() -> {
            UserCalendarSettings settings = new UserCalendarSettings();
            settings.setUser(user);
            return settingsRepo.save(settings);
        });
    }

    private Map<String, Object> getAvailabilityForUser(User user, boolean persistIfMissing) {
        UserCalendarSettings settings = getOrCreateSettings(user);
        String raw = settings.getWeeklyAvailabilityJson();
        Map<String, Object> availability = null;

        if (raw != null && !raw.isBlank()) {
            try {
                availability = objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                availability = null;
            }
        }

        if (availability == null) {
            availability = defaultAvailability();
            if (persistIfMissing) {
                try {
                    settings.setWeeklyAvailabilityJson(objectMapper.writeValueAsString(availability));
                    settingsRepo.save(settings);
                } catch (Exception e) {
                    // Keep request flow alive even if persistence fails.
                }
            }
        }

        return normalizeAvailability(availability);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> normalizeAvailability(Map<String, Object> source) {
        Map<String, Object> availability = new LinkedHashMap<>();
        String timezone = source != null && source.get("timezone") != null
                ? String.valueOf(source.get("timezone"))
                : ZoneId.systemDefault().getId();
        availability.put("timezone", timezone);
        availability.put("videoDatesEnabled", readBoolean(source != null ? source.get("videoDatesEnabled") : null, true));
        availability.put("minimumNoticeHours", readInt(source != null ? source.get("minimumNoticeHours") : null, 24, 0, 336));

        Map<String, Object> sourceDays = source != null && source.get("days") instanceof Map<?, ?> map
                ? (Map<String, Object>) map
                : Collections.emptyMap();

        Map<String, Object> days = new LinkedHashMap<>();
        for (String day : DAY_KEYS) {
            Map<String, Object> defaultDay = defaultDayAvailability(day);
            Map<String, Object> sourceDay = sourceDays.get(day) instanceof Map<?, ?> map
                    ? (Map<String, Object>) map
                    : Collections.emptyMap();

            Map<String, Object> normalizedDay = new LinkedHashMap<>();
            normalizedDay.put("enabled", readBoolean(sourceDay.get("enabled"), (Boolean) defaultDay.get("enabled")));
            normalizedDay.put("slots", normalizeSlots(sourceDay.get("slots"), (List<Map<String, String>>) defaultDay.get("slots")));
            days.put(day, normalizedDay);
        }
        availability.put("days", days);
        return availability;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> normalizeSlots(Object rawSlots, List<Map<String, String>> fallback) {
        List<Map<String, String>> normalized = new ArrayList<>();
        if (rawSlots instanceof List<?> slotList) {
            for (Object slotObj : slotList) {
                if (!(slotObj instanceof Map<?, ?> slotMap)) {
                    continue;
                }
                String start = slotMap.get("start") != null ? String.valueOf(slotMap.get("start")) : null;
                String end = slotMap.get("end") != null ? String.valueOf(slotMap.get("end")) : null;
                if (isValidTime(start) && isValidTime(end)) {
                    Map<String, String> slot = new LinkedHashMap<>();
                    slot.put("start", normalizeTime(start));
                    slot.put("end", normalizeTime(end));
                    normalized.add(slot);
                }
            }
        }

        if (!normalized.isEmpty()) {
            return normalized;
        }
        return fallback != null ? fallback : List.of();
    }

    private Map<String, Object> defaultAvailability() {
        Map<String, Object> availability = new LinkedHashMap<>();
        availability.put("timezone", ZoneId.systemDefault().getId());
        Map<String, Object> days = new LinkedHashMap<>();
        for (String day : DAY_KEYS) {
            days.put(day, defaultDayAvailability(day));
        }
        availability.put("days", days);
        availability.put("videoDatesEnabled", true);
        availability.put("minimumNoticeHours", 24);
        return availability;
    }

    private Map<String, Object> defaultDayAvailability(String day) {
        Map<String, Object> dayAvailability = new LinkedHashMap<>();
        dayAvailability.put("enabled", !"sunday".equals(day));
        dayAvailability.put("slots", List.of(Map.of("start", "17:00", "end", "21:00")));
        return dayAvailability;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> buildSlotsForDay(
            LocalDate day,
            Map<String, Object> userAvailability,
            Map<String, Object> partnerAvailability) {

        if (!readBoolean(userAvailability.get("videoDatesEnabled"), true) ||
            !readBoolean(partnerAvailability.get("videoDatesEnabled"), true)) {
            return List.of();
        }

        ZoneId userZone = readZone(userAvailability.get("timezone"));
        ZoneId partnerZone = readZone(partnerAvailability.get("timezone"));
        int minNoticeHours = Math.max(
                readInt(userAvailability.get("minimumNoticeHours"), 24, 0, 336),
                readInt(partnerAvailability.get("minimumNoticeHours"), 24, 0, 336)
        );

        List<TimeRange> userRanges = buildRangesForDay(day, userAvailability, userZone);
        List<TimeRange> partnerRanges = buildRangesForDay(day, partnerAvailability, partnerZone);
        if (userRanges.isEmpty() || partnerRanges.isEmpty()) {
            return List.of();
        }

        List<TimeRange> overlaps = intersectRanges(userRanges, partnerRanges);
        if (overlaps.isEmpty()) {
            return List.of();
        }

        Instant earliestAllowed = Instant.now().plus(minNoticeHours, ChronoUnit.HOURS);
        List<Map<String, Object>> slots = new ArrayList<>();
        for (TimeRange overlap : overlaps) {
            Instant cursor = ceilToSlot(maxInstant(overlap.start(), earliestAllowed));
            while (!cursor.plus(SLOT_MINUTES, ChronoUnit.MINUTES).isAfter(overlap.end())) {
                Instant slotEnd = cursor.plus(SLOT_MINUTES, ChronoUnit.MINUTES);
                slots.add(Map.of(
                        "startTime", cursor.toString(),
                        "endTime", slotEnd.toString(),
                        "available", true
                ));
                cursor = cursor.plus(SLOT_MINUTES, ChronoUnit.MINUTES);
            }
        }

        return slots.stream()
                .sorted(Comparator.comparing(slot -> Instant.parse(String.valueOf(slot.get("startTime")))))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<TimeRange> buildRangesForDay(LocalDate day, Map<String, Object> availability, ZoneId zone) {
        String dayKey = day.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase(Locale.ENGLISH);
        Object daysRaw = availability.get("days");
        if (!(daysRaw instanceof Map<?, ?> daysMapRaw)) {
            return List.of();
        }
        Object dayRaw = ((Map<String, Object>) daysMapRaw).get(dayKey);
        if (!(dayRaw instanceof Map<?, ?> dayMapRaw)) {
            return List.of();
        }

        Map<String, Object> dayMap = (Map<String, Object>) dayMapRaw;
        if (!readBoolean(dayMap.get("enabled"), false)) {
            return List.of();
        }

        Object slotsRaw = dayMap.get("slots");
        if (!(slotsRaw instanceof List<?> slots)) {
            return List.of();
        }

        List<TimeRange> ranges = new ArrayList<>();
        for (Object slotRaw : slots) {
            if (!(slotRaw instanceof Map<?, ?> slotMapRaw)) {
                continue;
            }
            Map<String, Object> slotMap = (Map<String, Object>) slotMapRaw;
            String startRaw = slotMap.get("start") != null ? String.valueOf(slotMap.get("start")) : null;
            String endRaw = slotMap.get("end") != null ? String.valueOf(slotMap.get("end")) : null;
            if (!isValidTime(startRaw) || !isValidTime(endRaw)) {
                continue;
            }

            LocalTime start = LocalTime.parse(normalizeTime(startRaw), TIME_FORMAT);
            LocalTime end = LocalTime.parse(normalizeTime(endRaw), TIME_FORMAT);

            LocalDateTime startDateTime = LocalDateTime.of(day, start);
            LocalDateTime endDateTime = LocalDateTime.of(day, end);
            if (!end.isAfter(start)) {
                endDateTime = endDateTime.plusDays(1);
            }

            ranges.add(new TimeRange(
                    startDateTime.atZone(zone).toInstant(),
                    endDateTime.atZone(zone).toInstant()
            ));
        }

        return ranges;
    }

    private List<TimeRange> intersectRanges(List<TimeRange> left, List<TimeRange> right) {
        List<TimeRange> overlaps = new ArrayList<>();
        for (TimeRange a : left) {
            for (TimeRange b : right) {
                Instant start = maxInstant(a.start(), b.start());
                Instant end = minInstant(a.end(), b.end());
                if (start.isBefore(end)) {
                    overlaps.add(new TimeRange(start, end));
                }
            }
        }
        return overlaps;
    }

    private Instant ceilToSlot(Instant instant) {
        long epochSeconds = instant.getEpochSecond();
        long slotSeconds = SLOT_MINUTES * 60L;
        long rounded = ((epochSeconds + slotSeconds - 1) / slotSeconds) * slotSeconds;
        return Instant.ofEpochSecond(rounded);
    }

    private Instant maxInstant(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private Instant minInstant(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    private ZoneId readZone(Object rawTimezone) {
        if (rawTimezone == null) {
            return ZoneId.systemDefault();
        }
        try {
            return ZoneId.of(String.valueOf(rawTimezone));
        } catch (Exception e) {
            return ZoneId.systemDefault();
        }
    }

    private boolean readBoolean(Object value, boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean b) return b;
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private int readInt(Object value, int defaultValue, int min, int max) {
        int parsed = defaultValue;
        if (value instanceof Number n) {
            parsed = n.intValue();
        } else if (value != null) {
            try {
                parsed = Integer.parseInt(String.valueOf(value));
            } catch (NumberFormatException ignored) {
                parsed = defaultValue;
            }
        }
        return Math.max(min, Math.min(max, parsed));
    }

    private boolean isValidTime(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            LocalTime.parse(value.trim(), TIME_FORMAT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String normalizeTime(String value) {
        LocalTime parsed = LocalTime.parse(value.trim(), TIME_FORMAT);
        return parsed.format(TIME_FORMAT);
    }

    private record TimeRange(Instant start, Instant end) {
    }
}
