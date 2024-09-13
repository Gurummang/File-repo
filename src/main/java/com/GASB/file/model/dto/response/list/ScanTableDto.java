package com.GASB.file.model.dto.response.list;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanTableDto {

    private boolean detect;
    private String yara;
}
