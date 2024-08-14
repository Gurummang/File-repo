package com.GASB.file.model.dto.response.list;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
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

    @Builder
    public VtReportDto(String type, String sha256, String v3, String alyac, String kaspersky, String falcon, String avast, String sentinelone, int detectEngine, int completeEngine, int score, String threatLabel, String reportUrl) {
        this.type = type;
        this.sha256 = sha256;
        this.v3 = v3;
        this.alyac = alyac;
        this.kaspersky = kaspersky;
        this.falcon = falcon;
        this.avast = avast;
        this.sentinelone = sentinelone;
        this.detectEngine = detectEngine;
        this.completeEngine = completeEngine;
        this.score = score;
        this.threatLabel = threatLabel;
        this.reportUrl = reportUrl;
    }
}
