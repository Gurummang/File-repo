package com.GASB.file.service.dashboard;

import com.GASB.file.model.dto.response.dashboard.FileDashboardDto;
import com.GASB.file.model.dto.response.dashboard.StatisticsDto;
import com.GASB.file.model.dto.response.dashboard.TotalTypeDto;
import com.GASB.file.repository.file.FileUploadRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileBoardReturnService {

    private FileUploadRepo fileUploadRepo;

    @Autowired
    public FileBoardReturnService(FileUploadRepo fileUploadRepo){
        this.fileUploadRepo = fileUploadRepo;
    }

    public FileDashboardDto boardListReturn(long org_saas_id){
        // 가상의 데이터를 사용하여 FileDashboardDto를 생성합니다.
        int totalCount = totalFilesCount(org_saas_id);
        double totalVolume = totalFileSizeCount(org_saas_id);
        int totalDlp = 5;
        int totalMalware = 2;

        // TotalTypeDto와 StatisticsDto의 리스트를 생성
        List<TotalTypeDto> totalTypeDtos = List.of(
                new TotalTypeDto("Type1", 50),
                new TotalTypeDto("Type2", 50)
        );

        List<StatisticsDto> statisticsDtos = List.of(
                new StatisticsDto("2024-01-01", 2.3f, 30),
                new StatisticsDto("2024-01-02", 4.0f, 24)
        );

        // FileDashboardDto 객체를 생성하고 반환
        return FileDashboardDto.builder()
                .total_count(totalCount)
                .total_volume(totalVolume)
                .total_dlp(totalDlp)
                .total_malware(totalMalware)
                .total_type(totalTypeDtos)
                .statistics(statisticsDtos)
                .build();
    }

    private int totalFilesCount(long org_saas_id){
        return fileUploadRepo.countByOrgSaasId(org_saas_id);
    }

    private double totalFileSizeCount(long org_saas_id){
        double totalSizeInBytes = fileUploadRepo.sumFileSizeByOrgSaasId(org_saas_id);
        double totalSizeInGB = totalSizeInBytes / 1_073_741_824.0;
        return Math.round(totalSizeInGB * 1000) / 1000.0;
    }
}
