package com.GASB.file.model.dto.response.list;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class FileStatusDto {

    private int gscanStatus;
    private int dlpStatus;
    private int vtStatus;

    @Builder
    public FileStatusDto(int gscanStatus, int dlpStatus, int vtStatus){
        this.gscanStatus = gscanStatus;
        this.dlpStatus = dlpStatus;
        this.vtStatus = vtStatus;
    }
}
