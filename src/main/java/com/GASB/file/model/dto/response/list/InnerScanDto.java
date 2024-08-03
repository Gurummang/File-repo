package com.GASB.file.model.dto.response.list;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class InnerScanDto {
    private String step2Detail;

    @Builder
    public InnerScanDto(String step2Detail){
        this.step2Detail = step2Detail;
    }
}
