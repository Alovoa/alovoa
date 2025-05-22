package com.nonononoki.alovoa.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class UserPromptDto {
    private Long promptId;
    private String text;
}
