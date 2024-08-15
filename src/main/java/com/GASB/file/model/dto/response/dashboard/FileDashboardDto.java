package com.GASB.file.model.dto.response.dashboard;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class FileDashboardDto {
    private long totalCount;
    private long totalVolume;
    private int totalDlp;
    private int totalMalware;
    private List<TotalTypeDto> totalType;
    private List<StatisticsDto> statistics;

    @Builder
    public FileDashboardDto(long totalCount, long totalVolume, int totalDlp, int totalMalware, List<TotalTypeDto> totalType, List<StatisticsDto> statistics){
        this.totalCount = totalCount;
        this.totalVolume = totalVolume;
        this.totalDlp = totalDlp;
        this.totalMalware = totalMalware;
        this.totalType = totalType;
        this.statistics = statistics;
    }
}
