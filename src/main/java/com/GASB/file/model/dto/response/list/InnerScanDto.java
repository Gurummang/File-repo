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

    private MimeTypeDto step1;
    private ScanTableDto step2;

    @Builder
    public InnerScanDto(MimeTypeDto step1, ScanTableDto step2){
        this.step1 = step1;
        this.step2 = step2;
    }
}
