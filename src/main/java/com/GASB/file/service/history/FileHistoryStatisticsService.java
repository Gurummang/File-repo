package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.dashboard.StatisticsDto;
import com.GASB.file.model.dto.response.history.FileHistoryStatistics;
import com.GASB.file.model.dto.response.history.FileHistoryTotalDto;
import com.GASB.file.repository.file.ActivitiesRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FileHistoryStatisticsService {

    private final ActivitiesRepo activitiesRepo;

    public FileHistoryStatisticsService(ActivitiesRepo activitiesRepo){
        this.activitiesRepo = activitiesRepo;
    }

    public FileHistoryTotalDto eventStatistics(long orgId){
        int totalUpload = getTotalUploadCount(orgId);
        int totalDeleted = getTotalDeletedCount(orgId);
        int totalChanged = getTotalChangedCount(orgId);
        int totalMoved = getTotalMovedCount(orgId);

        // FileHistoryStatistics 리스트 생성
        List<FileHistoryStatistics> fileHistoryStatisticsList = getFileHistoryStatisticsMonth(orgId);

        // FileHistoryTotalDto 객체 생성 및 반환
        return FileHistoryTotalDto.builder()
                .totalUpload(totalUpload)
                .totalDeleted(totalDeleted)
                .totalChanged(totalChanged)
                .totalMoved(totalMoved)
                .fileHistoryStatistics(fileHistoryStatisticsList)  // 리스트로 변경된 부분
                .build();
    }

    private int getTotalUploadCount(long orgId){
        return activitiesRepo.findTotalUploadCount(orgId);
    }

    private int getTotalDeletedCount(long orgId){
        return activitiesRepo.findTotalDeletedCount(orgId);
    }

    private int getTotalChangedCount(long orgId){
        return activitiesRepo.findTotalChangedCount(orgId);
    }

    private int getTotalMovedCount(long orgId){
        return activitiesRepo.findTotalMovedCount(orgId);
    }

    private List<FileHistoryStatistics> getFileHistoryStatisticsMonth(long orgId) {
        List<LocalDate> allDates = getLast30Days();
        LocalDateTime startDateTime = allDates.get(0).atStartOfDay();
        LocalDateTime endDateTime = allDates.get(allDates.size() - 1).atTime(LocalTime.MAX);

        List<Object[]> results = activitiesRepo.findFileHistoryStatistics(orgId, startDateTime, endDateTime);

        Map<LocalDate, FileHistoryStatistics> statisticsMap = new HashMap<>();

        for (Object[] row : results) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate(); // java.sql.Date를 LocalDate로 변환
            int uploadCount = ((Number) row[1]).intValue();
            int modifyCount = ((Number) row[2]).intValue();
            int deletedCount = ((Number) row[3]).intValue();
            int movedCount = ((Number) row[4]).intValue();

            FileHistoryStatistics stats = FileHistoryStatistics.builder()
                    .date(date.toString())
                    .uploadCount(uploadCount)
                    .deletedCount(deletedCount)
                    .modifyCount(modifyCount)
                    .movedCount(movedCount)
                    .build();

            statisticsMap.put(date, stats);
        }

        return allDates.stream()
                .map(date -> statisticsMap.getOrDefault(date, FileHistoryStatistics.builder()
                        .date(date.toString())
                        .uploadCount(0)
                        .deletedCount(0)
                        .modifyCount(0)
                        .movedCount(0)
                        .build()))
                .collect(Collectors.toList());
    }


    private List<LocalDate> getLast30Days() {
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