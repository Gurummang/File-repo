package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.FileHistoryBySaaS;
import com.GASB.file.model.dto.response.history.FileHistoryCorrelation;
import com.GASB.file.model.dto.response.history.FileHistoryDto;
import com.GASB.file.model.dto.response.history.FileHistoryListDto;
import com.GASB.file.model.entity.Activities;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileGroupRepo;
import com.GASB.file.repository.file.FileUploadRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class FileHistoryService {

    private final ActivitiesRepo activitiesRepo;
    private final FileGroupRepo fileGroupRepo;
    private final FileUploadRepo fileUploadRepo;

    public FileHistoryService(ActivitiesRepo activitiesRepo, FileGroupRepo fileGroupRepo, FileUploadRepo fileUploadRepo){
        this.activitiesRepo = activitiesRepo;
        this.fileGroupRepo = fileGroupRepo;
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

    public FileHistoryBySaaS createFileHistoryCorrelations(long activityId) {
        // 1. activityId를 사용하여 orgId를 조회합니다.
        Long orgId = activitiesRepo.findOrgIdByActivityId(activityId);
        if (orgId == null) {
            // orgId를 찾지 못한 경우, 적절한 예외 처리 또는 빈 결과를 반환합니다.
            return new FileHistoryBySaaS(List.of(), List.of());
        }

        // 2. activityId로 그룹 이름을 조회합니다.
        String groupName = fileGroupRepo.findGroupNameById(activityId);

        // 3. orgId와 groupName으로 Activities를 조회합니다.
        List<Activities> activitiesList = activitiesRepo.findAllByOrgIdAndGroupName(orgId, groupName);

        // 4. Slack과 Google Drive로 필터링합니다.
        List<FileHistoryCorrelation> slackHistories = filterHistoriesBySaas(activitiesList, "slack");
        List<FileHistoryCorrelation> googleDriveHistories = filterHistoriesBySaas(activitiesList, "googleDrive");

        // 5. FileHistoryBySaaS 객체를 생성하여 반환합니다.
        return new FileHistoryBySaaS(slackHistories, googleDriveHistories);
    }

    private List<FileHistoryCorrelation> filterHistoriesBySaas(List<Activities> activitiesList, String saasName) {
        return activitiesList.stream()
                .filter(activity -> activity.getUser().getOrgSaaS().getSaas().getSaasName().equals(saasName))
                .map(activity -> FileHistoryCorrelation.builder()
                        .eventId(activity.getId())
                        .saas(activity.getUser().getOrgSaaS().getSaas().getSaasName())
                        .eventType(activity.getEventType())
                        .fileName(activity.getFileName())
                        .hash256(getSaltedHash(activity.getUser().getOrgSaaS().getId(), activity.getSaasFileId()))
                        .saasFileId(activity.getSaasFileId())
                        .eventTs(activity.getEventTs())
                        .email(activity.getUser().getEmail())
                        .uploadChannel(activity.getUploadChannel())
                        .build())
                .collect(Collectors.toList());
    }

    private String getSaltedHash(long orgSaaSId, String saasFileId) {
        return fileUploadRepo.findHashByOrgSaaS_IdAndSaasFileId(orgSaaSId, saasFileId);
    }
}