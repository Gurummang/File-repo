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
    private int total_count;
    private double total_volume;
    private int total_dlp;
    private int total_malware;
    private List<TotalTypeDto> totalTypeDto;
    private List<StatisticsDto> statisticsDto;

    @Builder
    public FileDashboardDto(int total_count, double total_volume, int total_dlp, int total_malware, List<TotalTypeDto> totalTypeDto, List<StatisticsDto> statisticsDto){
        this.total_count = total_count;
        this.total_volume = total_volume;
        this.total_dlp = total_dlp;
        this.total_malware = total_malware;
        this.totalTypeDto = totalTypeDto;
        this.statisticsDto = statisticsDto;
    }
}
