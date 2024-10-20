package com.GASB.file.model.dto.response.list;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanTableDto {

    private boolean detect;
    private List<String> yara;
}
