package com.GASB.file.service.dashboard;

import com.GASB.file.model.dto.response.dashboard.FileDashboardDto;
import com.GASB.file.model.dto.response.dashboard.StatisticsDto;
import com.GASB.file.model.dto.response.dashboard.TotalTypeDto;
import com.GASB.file.repository.file.FileUploadRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class FileBoardReturnService {

    private final FileUploadRepo fileUploadRepo;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    public FileBoardReturnService(FileUploadRepo fileUploadRepo){
        this.fileUploadRepo = fileUploadRepo;
    }

    public FileDashboardDto boardListReturn(long orgId) {

        long totalCount = totalFilesCount(orgId);
        long totalVolume = totalFileSizeCount(orgId);
        int totalDlp = totalDlpCount(orgId);
        int totalMalware = totalMalwareCount(orgId);

        List<TotalTypeDto> totalType = getFileTypeDistribution(orgId);
        List<StatisticsDto> statistics = getFileStatisticsMonth(orgId);

        // FileDashboardDto 객체를 생성하고 반환
        return FileDashboardDto.builder()
                .totalCount(totalCount)
                .totalVolume(totalVolume)
                .totalDlp(totalDlp)
                .totalMalware(totalMalware)
                .totalType(totalType != null ? totalType : Collections.emptyList())
                .statistics(statistics != null ? statistics : Collections.emptyList())
                .build();
    }

    private long totalFilesCount(long orgId) {
        // null을 처리하여 기본값 0L을 반환
        Long count = fileUploadRepo.countFileByOrgId(orgId);
        return count != null ? count : 0L;
    }

    private long totalFileSizeCount(long orgId) {
        // null을 처리하여 기본값 0L을 반환
        Long totalSize = fileUploadRepo.getTotalSizeByOrgId(orgId);
        return totalSize != null ? totalSize : 0L;
    }

    private int totalDlpCount(long orgId) {
        // null을 처리하여 기본값 0을 반환
        Integer count = fileUploadRepo.countDlpIssuesByOrgId(orgId);
        return count != null ? count : 0;
    }

    private int totalMalwareCount(long orgId) {
        // null을 처리하여 기본값 0을 반환
        Integer countVtMalware = fileUploadRepo.countVtMalwareByOrgId(orgId);
        Integer countSuspiciousMalware = fileUploadRepo.countSuspiciousMalwareByOrgId(orgId);
        return (countVtMalware != null ? countVtMalware : 0) +
                (countSuspiciousMalware != null ? countSuspiciousMalware : 0);
    }

    private List<TotalTypeDto> getFileTypeDistribution(long orgId) {
        // null을 처리하여 기본값 빈 리스트를 반환
        List<TotalTypeDto> totalType = fileUploadRepo.findFileTypeDistributionByOrgId(orgId);
        return totalType != null ? totalType : Collections.emptyList();
    }

    public List<StatisticsDto> getFileStatisticsMonth(long orgId) {
        List<LocalDate> allDates = getLast30Days();
        LocalDateTime startDateTime = allDates.get(0).atStartOfDay();
        LocalDateTime endDateTime = allDates.get(allDates.size() - 1).atTime(LocalTime.MAX);

        List<Object[]> results = fileUploadRepo.findStatistics(orgId, startDateTime, endDateTime);

        Map<LocalDate, StatisticsDto> statisticsMap = new HashMap<>();

        for (Object[] row : results) {
            LocalDateTime timestamp = (LocalDateTime) row[0];
            LocalDate date = timestamp.toLocalDate();
            long totalSizeInBytes = ((Number) row[1]).longValue();
            long fileCount = ((Number) row[2]).longValue();

            StatisticsDto dto = statisticsMap.getOrDefault(date, new StatisticsDto(
                    date.format(dateFormatter),
                    0,
                    0
            ));

            dto.setCount(dto.getCount() + (int) fileCount);
            dto.setVolume(dto.getVolume() + totalSizeInBytes);

            statisticsMap.put(date, dto);
        }

        return allDates.stream()
                .map(date -> statisticsMap.getOrDefault(date, new StatisticsDto(
                        date.format(dateFormatter),
                        0,
                        0
                )))
                .toList();
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

