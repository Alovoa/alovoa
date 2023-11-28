package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.model.UserDto;

import java.util.List;

public interface SortStrategy {
    List<UserDto> sort(List<UserDto> userDtos);
}
