package com.GASB.file.model.dto.response.list;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VtReportDto {

    private String type;
    private String sha256;
    private String v3;
    private String alyac;
    private String kaspersky;
    private String falcon;
    private String avast;
    private String sentinelone;
    private int detectEngine;
    private int completeEngine;
    private int score;
    private String threatLabel;
    private String reportUrl;
}
