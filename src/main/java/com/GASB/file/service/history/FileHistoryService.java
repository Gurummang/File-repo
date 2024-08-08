package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.FileHistoryCorrelation;
import com.GASB.file.model.dto.response.history.FileHistoryDto;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class FileHistoryService {

    public List<FileHistoryDto> historyListReturn() {
        List<FileHistoryDto> fileHistoryDtoList = new ArrayList<>();

        // 예시로 5개의 FileHistoryDto 객체를 생성
        for (int i = 0; i < 5; i++) {
            long eventId = generateRandomEventId();
            String SaaS = "SampleSaaS";
            String eventType = "UPLOAD";
            String fileName = "example-file-" + i + ".txt";
            LocalDateTime uploadTs = LocalDateTime.now().minusDays(i);
            LocalDateTime eventTs = LocalDateTime.now();
            String email = "user" + i + "@example.com";
            String uploadChannel = "WEB";

            // FileHistoryCorrelation 리스트 생성
            List<FileHistoryCorrelation> correlationList = fileGroupList();

            // FileHistoryDto 객체 생성 및 리스트에 추가
            FileHistoryDto fileHistoryDto = FileHistoryDto.builder()
                    .eventId(eventId)
                    .saas(SaaS)
                    .eventType(eventType)
                    .fileName(fileName)
                    .uploadTs(uploadTs)
                    .eventTs(eventTs)
                    .email(email)
                    .uploadChannel(uploadChannel)
                    .correlation(correlationList)
                    .build();

            fileHistoryDtoList.add(fileHistoryDto);
        }

        return fileHistoryDtoList;
    }

    private List<FileHistoryCorrelation> fileGroupList() {
        // 샘플 FileHistoryCorrelation 객체 리스트 생성
        List<FileHistoryCorrelation> correlationList = new ArrayList<>();

        // 여러 개의 FileHistoryCorrelation 객체 생성 및 추가
        for (int i = 0; i < 3; i++) { // 예시로 3개의 객체를 추가
            FileHistoryCorrelation correlation = new FileHistoryCorrelation();
            // correlation 객체의 필드를 설정합니다. 예: correlation.setSomeField("value");
            correlationList.add(correlation);
        }

        return correlationList;
    }

    private long generateRandomEventId() {
        // 예시로 랜덤 이벤트 ID 생성
        return new Random().nextLong();
    }
}