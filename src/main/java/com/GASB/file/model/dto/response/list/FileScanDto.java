package com.GASB.file.model.dto.response.list;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileScanDto {
    // FileUpload
    private long fileUploadId;
    private String saasFileId;
    private LocalDateTime timestamp;

    // StoredFile
    private long storedFileId;
    private String hash;
    private Integer size;
    private String type;

    // DlpStat, FileStatus
    private Integer dlpStatus;
    private Integer vtStatus;
    private Integer gscanStatus;

    // TypeScan
    private Boolean correct;
    private String mimeType;
    private String signature;
    private String extension;

    // Gscan
    private Boolean detect;
    private String step2Detail;

    // Vt
    private String vtType;
    private String v3;
    private String alyac;
    private String kaspersky;
    private String falcon;
    private String avast;
    private String sentinelone;
    private Integer detectEngine;
    private Integer completeEngine;
    private Integer score;
    private String threatLabel;
    private String reportUrl;

    public FileScanDto(long fileUploadId, String saasFileId, LocalDateTime timestamp,
                       long storedFileId, String hash, Integer size, String type,
                       Integer dlpStatus, Integer vtStatus, Integer gscanStatus,
                       Boolean correct, String mimeType, String signature, String extension,
                       String vtType, String v3, String alyac, String kaspersky, String falcon,
                       String avast, String sentinelone, Integer detectEngine, Integer completeEngine,
                       Integer score, String threatLabel, String reportUrl,
                       Boolean detect, String step2Detail) {
        this.fileUploadId = fileUploadId;
        this.saasFileId = saasFileId;
        this.timestamp = timestamp;
        this.storedFileId = storedFileId;
        this.hash = hash;
        this.size = size;
        this.type = type;
        this.dlpStatus = dlpStatus;
        this.vtStatus = vtStatus;
        this.gscanStatus = gscanStatus;
        this.correct = correct;
        this.mimeType = mimeType;
        this.signature = signature;
        this.extension = extension;
        this.vtType = vtType;
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
        this.detect = detect;
        this.step2Detail = step2Detail;
    }
}
