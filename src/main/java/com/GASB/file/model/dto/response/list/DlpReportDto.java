package com.GASB.file.model.dto.response.list;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DlpReportDto {

    private List<PolicyDto> policies; // 걸린 정책 정보
    private boolean identify;
    private boolean passport;
    private boolean drive;
    private boolean foreigner;

}
