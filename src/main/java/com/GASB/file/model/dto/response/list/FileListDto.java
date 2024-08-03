package com.GASB.file.model.dto.response.list;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class FileListDto {
    private long id;
    private String saltedHash;
    private int size;
    private String type;
    private VtReportDto vtReport;
    private FileStatusDto fileStatus;
    private InnerScanDto GScan;


    @Builder
    public FileListDto(long id, String saltedHash, int size, String type, VtReportDto vtReport, FileStatusDto fileStatus, InnerScanDto GScan){
        this.id = id;
        this.saltedHash = saltedHash;
        this.size = size;
        this.type = type;
        this.vtReport = vtReport;
        this.fileStatus = fileStatus;
        this.GScan = GScan;
    }
}
