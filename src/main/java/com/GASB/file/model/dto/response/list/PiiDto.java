package com.GASB.file.model.dto.response.list;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PiiDto {

    private String pii;
    private int dlpCount;
}
