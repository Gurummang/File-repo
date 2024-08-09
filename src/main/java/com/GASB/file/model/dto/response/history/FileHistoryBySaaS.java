package com.GASB.file.model.dto.response.history;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class FileHistoryBySaaS {
    private List<FileHistoryCorrelation> slack;
    private List<FileHistoryCorrelation> googleDrive;

    public FileHistoryBySaaS(List<FileHistoryCorrelation> slack, List<FileHistoryCorrelation> googleDrive){
        this.slack = slack;
        this.googleDrive =googleDrive;
    }
}
