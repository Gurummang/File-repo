package com.GASB.file.model.dto.response.history;

import com.GASB.file.model.entity.Activities;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ExplorationNode {
    private final Activities activity;
    private final int depth;
}
