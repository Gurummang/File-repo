package com.GASB.file.model.dto.response.list;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DlpReportDto {

    private int totalPolicies; // 탐지 정책 수
    private int totalDlp; // 탐지 개수
    private List<String> comments; // 권장 조치
    private List<PolicyDto> policies; // 걸린 정책 정보
    private List<PiiDto> pii;
}
