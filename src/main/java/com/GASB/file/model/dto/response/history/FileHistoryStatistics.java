package com.GASB.file.model.dto.response.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileHistoryStatistics {
    private String date;
    private int uploadCount;
    private int deletedCount;
    private int modifyCount;
    private int movedCount;
}