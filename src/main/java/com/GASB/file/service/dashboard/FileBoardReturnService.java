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
import java.util.List;
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
                .total_count(totalCount)
                .total_volume(totalVolume)
                .total_dlp(totalDlp)
                .total_malware(totalMalware)
                .total_type(totalType)
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
        List<LocalDate> last30Days = getLast30Days();
        LocalDate startDate = last30Days.get(0);
        LocalDate endDate = last30Days.get(last30Days.size() - 1);

        // 시작 시간과 끝 시간을 설정하여 날짜 부분만 비교
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        List<Object[]> results = fileUploadRepo.findStatistics(orgId, startDateTime, endDateTime);

        return last30Days.stream()
                .map(date -> {
                    Object[] row = results.stream()
                            .filter(result -> {
                                LocalDateTime resultDate = (LocalDateTime) result[0];
                                return resultDate.toLocalDate().equals(date);
                            })
                            .findFirst()
                            .orElse(new Object[]{date.atStartOfDay(), 0.0, 0});
                    int totalSizeInBytes = ((Number) row[1]).intValue();
                    int fileCount = ((Number) row[2]).intValue();

                    return new StatisticsDto(
                            date.format(dateFormatter),
                            totalSizeInBytes,
                            fileCount
                    );
                })
                .collect(Collectors.toList());
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
