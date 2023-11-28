package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.model.UserDto;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class SortByDistance implements SortStrategy {
    @Override
    public List<UserDto> sort(List<UserDto> userDtos) {
        return userDtos.stream().sorted(Comparator.comparing(UserDto::getDistanceToUser))
                .collect(Collectors.toList());
    }
}
