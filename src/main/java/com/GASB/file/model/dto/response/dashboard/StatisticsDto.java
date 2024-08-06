package com.GASB.file.model.dto.response.dashboard;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class StatisticsDto {
    private String date;
    private int volume;
    private int count;

    @Builder
    public StatisticsDto(String date, int volume, int count){
        this.date = date;
        this.volume = volume;
        this.count = count;
    }
}
