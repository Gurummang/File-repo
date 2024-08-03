package com.GASB.file.model.dto.response.dashboard;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class TotalTypeDto {
    private String type;
    private int count;

    @Builder
    public TotalTypeDto(String type, int count){
        this.type = type;
        this.count = count;
    }
}
