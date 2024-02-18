package com.nonononoki.alovoa.model;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Builder
@Data
public class UserPromptDto {
    private Long promptId;
    private String text;
}
