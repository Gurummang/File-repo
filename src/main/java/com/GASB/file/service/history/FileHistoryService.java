package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.FileHistoryCorrelation;
import com.GASB.file.model.dto.response.history.FileHistoryDto;
import com.GASB.file.model.entity.Activities;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileGroupRepo;
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

    public FileHistoryService(ActivitiesRepo activitiesRepo, FileGroupRepo fileGroupRepo){
        this.activitiesRepo = activitiesRepo;
        this.fileGroupRepo = fileGroupRepo;
    }
    public List<FileHistoryDto> historyListReturn(long orgId) {
        // Activities 엔티티를 조회합니다.
        List<Activities> activitiesList = activitiesRepo.findByUser_OrgSaaS_Org_Id(orgId);

        // 각 Activities를 FileHistoryDto로 변환합니다.
        return activitiesList.stream()
                .map(activity -> convertToFileHistoryDto(activity, orgId))
                .sorted(Comparator.comparing(FileHistoryDto::getEventTs))
                .collect(Collectors.toList());
    }

    private FileHistoryDto convertToFileHistoryDto(Activities activity, long orgId) {
        // FileHistoryCorrelation 리스트를 생성합니다.
        List<FileHistoryCorrelation> correlations = createFileHistoryCorrelations(activity.getId(), orgId);

        // FileHistoryCorrelation 리스트를 eventTs 기준으로 정렬합니다.
        List<FileHistoryCorrelation> sortedCorrelations = correlations.stream()
                .sorted(Comparator.comparing(FileHistoryCorrelation::getEventTs))
                .toList();

        // FileHistoryDto를 생성합니다.
        return FileHistoryDto.builder()
                .eventId(activity.getId())
                .saas(activity.getUser().getOrgSaaS().getSaas().getSaasName())
                .eventType(activity.getEventType())
                .fileName(activity.getFileName())
                .saasFileId(activity.getSaasFileId())
                .uploadTs(activity.getEventTs())
                .eventTs(activity.getEventTs())
                .email(activity.getUser().getEmail())
                .uploadChannel(activity.getUploadChannel())
                .correlation(correlations)
                .build();
    }

    private List<FileHistoryCorrelation> createFileHistoryCorrelations(long activityId, long orgId) {
        // activityId를 기반으로 그룹 이름을 조회합니다.
        String groupName = fileGroupRepo.findGroupNameById(activityId);

        // orgId와 groupName으로 필터링된 Activities를 조회합니다.
        List<Activities> activities = activitiesRepo.findAllByOrgIdAndGroupName(orgId, groupName);

        // 일치하는 그룹 이름을 가진 FileHistoryCorrelation 객체 리스트를 생성합니다.
        return activities.stream()
                .map(activity -> FileHistoryCorrelation.builder()
                        .eventId(activity.getId())
                        .saas(activity.getUser().getOrgSaaS().getSaas().getSaasName())
                        .eventType(activity.getEventType())
                        .fileName(activity.getFileName())
                        .saasFileId(activity.getSaasFileId())
                        .eventTs(activity.getEventTs())
                        .email(activity.getUser().getEmail())
                        .uploadChannel(activity.getUploadChannel())
                        .build())
                .collect(Collectors.toList());
    }
}