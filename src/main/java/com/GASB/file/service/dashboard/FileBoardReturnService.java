package com.GASB.file.service.dashboard;

import com.GASB.file.model.dto.response.dashboard.FileDashboardDto;
import com.GASB.file.model.dto.response.dashboard.StatisticsDto;
import com.GASB.file.model.dto.response.dashboard.TotalTypeDto;
import com.GASB.file.model.entity.OrgSaaS;
import com.GASB.file.repository.file.FileUploadRepo;
import com.GASB.file.repository.org.OrgSaaSRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FileBoardReturnService {

    private final FileUploadRepo fileUploadRepo;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public FileBoardReturnService(FileUploadRepo fileUploadRepo){
        this.fileUploadRepo = fileUploadRepo;
    }

    public FileDashboardDto boardListReturn(long org_id){

        long totalCount = totalFilesCount(org_id);
        long totalVolume = totalFileSizeCount(org_id);
        int totalDlp = totalDlpCount(org_id);
        int totalMalware = totalMalwareCount(org_id);

        List<TotalTypeDto> totalType = getFileTypeDistribution(org_id);
        List<StatisticsDto> statistics = getFileStatisticsMonth(org_id);

        // FileDashboardDto 객체를 생성하고 반환
        return FileDashboardDto.builder()
                .totalCount(totalCount)
                .totalVolume(totalVolume)
                .totalDlp(totalDlp)
                .totalMalware(totalMalware)
                .totalType(totalType)
                .statistics(statistics)
                .build();
    }

    private long totalFilesCount(long org_id){
        return fileUploadRepo.countFileByOrgId(org_id);
    }

    private long totalFileSizeCount(long org_id){
        return fileUploadRepo.getTotalSizeByOrgId(org_id);
//        double totalSizeInBytes = fileUploadRepo.getTotalSizeByOrgId(org_id);
//        double totalSizeInGB = totalSizeInBytes / 1_073_741_824.0;
//        return Math.round(totalSizeInGB * 1000) / 1000.0;
    }

    private int totalDlpCount(long org_id){
        return fileUploadRepo.countDlpIssuesByOrgId(org_id);
    }

    private int totalMalwareCount(long org_id){
        return fileUploadRepo.countVtMalwareByOrgId(org_id) + fileUploadRepo.countSuspiciousMalwareByOrgId(org_id);
    }

    private List<TotalTypeDto> getFileTypeDistribution(long orgId) {
        // 리포지토리 메서드를 호출하여 파일 타입 분포를 가져옴
        return fileUploadRepo.findFileTypeDistributionByOrgId(orgId);
    }

    public List<StatisticsDto> getFileStatisticsMonth(long orgId) {
        List<LocalDate> allDates = getLast30Days();
        LocalDateTime startDateTime = allDates.get(0).atStartOfDay();
        LocalDateTime endDateTime = allDates.get(allDates.size() - 1).atTime(LocalTime.MAX);

        List<Object[]> results = fileUploadRepo.findStatistics(orgId, startDateTime, endDateTime);

        // 날짜별로 집계
        Map<LocalDate, StatisticsDto> statisticsMap = new HashMap<>();

        for (Object[] row : results) {
            // 결과에서 날짜와 집계 값 추출
            LocalDateTime timestamp = (LocalDateTime) row[0];
            LocalDate date = timestamp.toLocalDate();
            long totalSizeInBytes = ((Number) row[1]).longValue();
            long fileCount = ((Number) row[2]).longValue();

            // 날짜별로 통계 집계
            StatisticsDto dto = statisticsMap.getOrDefault(date, new StatisticsDto(
                    date.format(dateFormatter),
                    0,
                    0
            ));

            dto.setCount(dto.getCount() + (int) fileCount);
            dto.setVolume(dto.getVolume() + totalSizeInBytes);

            statisticsMap.put(date, dto);
        }

        // 모든 날짜를 포함하도록 날짜 범위와 매핑된 데이터를 결합
        List<StatisticsDto> resultStatistics = allDates.stream()
                .map(date -> {
                    StatisticsDto dto = statisticsMap.getOrDefault(date, new StatisticsDto(
                            date.format(dateFormatter),
                            0,
                            0
                    ));
                    return dto;
                })
                .collect(Collectors.toList());

        return resultStatistics;
    }


    public List<LocalDate> getLast30Days() {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);

        while (!startDate.isAfter(endDate)) {
            dates.add(startDate);
            startDate = startDate.plusDays(1);
        }

        return dates;
    }

}
