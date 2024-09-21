package com.GASB.file.model.dto.response.list;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileListDto {
    private long id;
    private String name;
    private int size;
    private String type;
    private String saas;
    private String user;
    private String path;
    private LocalDateTime date;
    private VtReportDto vtReport;
    private DlpReportDto dlpReport;
    private FileStatusDto fileStatus;
    private InnerScanDto gscan;
}
