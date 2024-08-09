package com.GASB.file.model.dto.response.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileHistoryTotalDto {
    private int totalUpload;
    private int totalDeleted;
    private int totalModify;
    private int totalMoved;
    private List<FileHistoryStatistics> fileHistoryStatistics;
}
