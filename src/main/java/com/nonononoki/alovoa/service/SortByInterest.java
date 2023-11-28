package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.model.UserDto;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SortByInterest implements SortStrategy {
    @Override
    public List<UserDto> sort(List<UserDto> userDtos) {
        Comparator<UserDto> comparatorCommonInterest = Comparator.comparing(f -> f.getCommonInterests().size());
        return userDtos.stream().filter(f -> !f.getCommonInterests().isEmpty())
                .sorted(comparatorCommonInterest.reversed()
                        .thenComparing(Comparator.comparing(UserDto::getDistanceToUser).reversed()))
                .collect(Collectors.toList());
    }
}
