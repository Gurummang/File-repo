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
    public FileDashboardDto(long total_count, long total_volume, int total_dlp, int total_malware, List<TotalTypeDto> total_type, List<StatisticsDto> statistics){
        this.totalCount = total_count;
        this.totalVolume = total_volume;
        this.totalDlp = total_dlp;
        this.totalMalware = total_malware;
        this.totalType = total_type;
        this.statistics = statistics;
    }
}
