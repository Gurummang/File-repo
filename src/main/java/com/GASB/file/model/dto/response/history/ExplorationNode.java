package com.GASB.file.model.dto.response.history;

import com.GASB.file.model.entity.Activities;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@Builder
public class ExplorationNode {
    private final Activities activity;
    private final int depth;
}
