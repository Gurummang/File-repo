package com.GASB.file.model.dto.response.list;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
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
    private FileStatusDto fileStatus;
    private InnerScanDto GScan;


    @Builder
    public FileListDto(long id, String fileName, int size, String type, String saas, String user, String uploadChannel, LocalDateTime created_at, VtReportDto vtReport, FileStatusDto fileStatus, InnerScanDto GScan){
        this.id = id;
        this.name = fileName;
        this.size = size;
        this.type = type;
        this.saas = saas;
        this.user = user;
        this.path = uploadChannel;
        this.date = created_at;
        this.vtReport = vtReport;
        this.fileStatus = fileStatus;
        this.GScan = GScan;
    }
}
