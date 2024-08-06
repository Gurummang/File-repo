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
    private long total_count;
    private long total_volume;
    private int total_dlp;
    private int total_malware;
    private List<TotalTypeDto> total_type;
    private List<StatisticsDto> statistics;

    @Builder
    public FileDashboardDto(long total_count, long total_volume, int total_dlp, int total_malware, List<TotalTypeDto> total_type, List<StatisticsDto> statistics){
        this.total_count = total_count;
        this.total_volume = total_volume;
        this.total_dlp = total_dlp;
        this.total_malware = total_malware;
        this.total_type = total_type;
        this.statistics = statistics;
    }
}
