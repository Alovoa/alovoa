package com.nonononoki.alovoa.matching.rerank.service;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SegmentKeyService {

    public String segmentKey(User user) {
        String gender = normalize(user != null && user.getGender() != null ? user.getGender().getText() : "unknown");
        String seeking = normalizePreferred(user != null ? user.getPreferedGenders() : null);
        String ageBucket = ageBucket(user);
        String geo = normalize(user != null ? user.getCountry() : null);

        return "gender:" + gender +
                "|seeking:" + seeking +
                "|age:" + ageBucket +
                "|geo:" + geo;
    }

    public String ageBucket(User user) {
        if (user == null || user.getDates() == null || user.getDates().getDateOfBirth() == null) {
            return "unknown";
        }
        LocalDate dob = Instant.ofEpochMilli(user.getDates().getDateOfBirth().getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        int age = Math.max(0, Period.between(dob, LocalDate.now()).getYears());

        if (age < 18) return "u18";
        if (age <= 24) return "18_24";
        if (age <= 29) return "25_29";
        if (age <= 34) return "30_34";
        if (age <= 39) return "35_39";
        if (age <= 49) return "40_49";
        return "50_plus";
    }

    private String normalizePreferred(Set<Gender> genders) {
        if (genders == null || genders.isEmpty()) {
            return "unknown";
        }
        return genders.stream()
                .map(Gender::getText)
                .filter(s -> s != null && !s.isBlank())
                .map(this::normalize)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.joining("+"));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }
}
