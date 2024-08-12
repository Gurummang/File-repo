package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.FileHistoryDto;
import com.GASB.file.model.dto.response.history.FileHistoryListDto;
import com.GASB.file.model.entity.Activities;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileUploadRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FileHistoryService {

    private final ActivitiesRepo activitiesRepo;
    private final FileUploadRepo fileUploadRepo;

    public FileHistoryService(ActivitiesRepo activitiesRepo, FileUploadRepo fileUploadRepo){
        this.activitiesRepo = activitiesRepo;
        this.fileUploadRepo = fileUploadRepo;
    }

    // 역사 리스트 반환 메서드
    public List<FileHistoryListDto> historyListReturn(long orgId) {

        List<Activities> activitiesList = activitiesRepo.findByUser_OrgSaaS_Org_Id(orgId);

        List<FileHistoryDto> sortedHistoryList = activitiesList.stream()
                .map(this::convertToFileHistoryDto)
                .sorted(Comparator.comparing(FileHistoryDto::getEventTs))
                .collect(Collectors.toList());

        int totalEvent = sortedHistoryList.size();

        FileHistoryListDto fileHistoryListDto = FileHistoryListDto.builder()
                .totalEvent(totalEvent)
                .fileHistoryDto(sortedHistoryList)
                .build();

        return List.of(fileHistoryListDto);
    }


    private FileHistoryDto convertToFileHistoryDto(Activities activity) {
        LocalDateTime uploadTs = fileUploadRepo.findUploadTsByOrgSaaS_IdAndSaasFileId(activity.getUser().getOrgSaaS().getId(),activity.getSaasFileId());
        return FileHistoryDto.builder()
                .eventId(activity.getId())
                .saas(activity.getUser().getOrgSaaS().getSaas().getSaasName())
                .eventType(activity.getEventType())
                .fileName(activity.getFileName())
                .saasFileId(activity.getSaasFileId())
                .uploadTs(uploadTs)
                .eventTs(activity.getEventTs())
                .email(activity.getUser().getEmail())
                .uploadChannel(activity.getUploadChannel())
                .build();
    }
}