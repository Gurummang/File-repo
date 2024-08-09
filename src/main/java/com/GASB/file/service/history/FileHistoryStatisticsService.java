package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.FileHistoryStatistics;
import com.GASB.file.model.dto.response.history.FileHistoryTotalDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FileHistoryStatisticsService {

    public FileHistoryTotalDto eventStatistics(){
        int totalUpload = 100;
        int totalDeleted = 40;
        int totalModify = 20;
        int totalMoved = 10;

        // FileHistoryStatistics 리스트 생성
        List<FileHistoryStatistics> fileHistoryStatisticsList = new ArrayList<>();

        // 예제 데이터를 리스트에 추가
        fileHistoryStatisticsList.add(FileHistoryStatistics.builder()
                .date("2024-08-08")
                .uploadCount(15)
                .deletedCount(5)
                .modifyCount(7)
                .movedCount(3)
                .build());

        fileHistoryStatisticsList.add(FileHistoryStatistics.builder()
                .date("2024-08-09")
                .uploadCount(10)
                .deletedCount(5)
                .modifyCount(3)
                .movedCount(2)
                .build());

        fileHistoryStatisticsList.add(FileHistoryStatistics.builder()
                .date("2024-08-10")
                .uploadCount(20)
                .deletedCount(10)
                .modifyCount(5)
                .movedCount(4)
                .build());

        // FileHistoryTotalDto 객체 생성 및 반환
        return FileHistoryTotalDto.builder()
                .totalUpload(totalUpload)
                .totalDeleted(totalDeleted)
                .totalModify(totalModify)
                .totalMoved(totalMoved)
                .fileHistoryStatistics(fileHistoryStatisticsList)  // 리스트로 변경된 부분
                .build();
    }
}