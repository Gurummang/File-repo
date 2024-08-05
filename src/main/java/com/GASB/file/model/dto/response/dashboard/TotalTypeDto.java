package com.GASB.file.model.dto.response.dashboard;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TotalTypeDto {
    private String type;
    private Long count;

    public TotalTypeDto(String type, Long count){
        this.type = type;
        this.count = count;
    }
}
