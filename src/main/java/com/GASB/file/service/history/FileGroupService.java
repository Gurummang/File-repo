package com.GASB.file.service.history;

import com.GASB.file.model.entity.*;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileGroupRepo;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileGroupService {

    private static final double SIM_THRESHOLD = 0.8;

    private final ActivitiesRepo activitiesRepo;
    private final FileGroupRepo fileGroupRepo;

    @Autowired
    public FileGroupService(ActivitiesRepo activitiesRepo, FileGroupRepo fileGroupRepo) {
        this.activitiesRepo = activitiesRepo;
        this.fileGroupRepo = fileGroupRepo;
    }

    // 유사도 측정 메서드
    private double calculateSimilarity(String a, String b) {
        JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();
        return similarity.apply(a, b);
    }

    // 확장자를 제거한 파일 이름을 반환하는 메서드
    private String getFileNameWithoutExtension(String fileName) {
        return FilenameUtils.getBaseName(fileName).toLowerCase();  // Convert to lower case for consistency
    }

    // 파일의 확장자를 기반으로 그룹 유형을 결정하는 메서드
    private String determineFileType(String fileName) {
        String extension = FilenameUtils.getExtension(fileName).toLowerCase();
        return switch (extension) {
            case "exe", "dll", "elf" -> "execute";
            case "jpg", "jpeg", "png", "gif", "webp", "svg" -> "image";
            case "docx", "hwp", "doc", "xls", "xlsx", "ppt", "pptx", "pdf", "text", "txt", "html" -> "document";
            default -> "unknown"; // 기타 확장자는 "unknown"으로 처리
        };
    }

    public void groupFilesAndSave(long actId) {
        // 1. 파일 ID로 Activities 객체 조회
        Activities activity = activitiesRepo.findById(actId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // 2. 현재 검사 주체의 파일 이름과 확장자
        String actFileName = getFileNameWithoutExtension(activity.getFileName());
        String actFileType = determineFileType(activity.getFileName());
        Timestamp actFileTs = Timestamp.valueOf(activity.getEventTs());

        // 3. orgId 조회
        long orgId = activity.getUser().getOrgSaaS().getOrg().getId();

        // 4. orgId와 일치하고, type이 동일한 활동들 가져오기 (actId 제외)
        List<Activities> selectedActivities = activitiesRepo.findAll().stream()
                .filter(a -> a.getId() != actId) // 현재 파일을 제외
                .filter(a -> {
                    MonitoredUsers otherMonitoredUsers = a.getUser();
                    return otherMonitoredUsers != null &&
                            otherMonitoredUsers.getOrgSaaS().getOrg().getId() == orgId &&
                            determineFileType(a.getFileName()).equals(actFileType); // 타입 일치
                })
                .distinct() // 중복 제거
                .toList();

        // 5. 그룹 이름 추출 및 null과 중복 제거
        Set<String> groupNames = selectedActivities.stream()
                .map(a -> fileGroupRepo.findGroupNameById(a.getId())) // groupName 조회
                .filter(Objects::nonNull) // null 제거
                .collect(Collectors.toSet()); // 중복 제거

        boolean groupUpdated = false;

        for (String groupName : groupNames) {
            double similarity = calculateSimilarity(actFileName, groupName);

            if (similarity >= SIM_THRESHOLD) {
                // 그룹의 파일들 중 가장 빠른 타임스탬프 찾기
                List<Activities> groupActivities = selectedActivities.stream()
                        .filter(a -> groupName.equals(fileGroupRepo.findGroupNameById(a.getId())))
                        .toList();

                Timestamp earliestTs = groupActivities.stream()
                        .map(a -> Timestamp.valueOf(a.getEventTs()))
                        .min(Comparator.naturalOrder())
                        .orElse(null);

                if (earliestTs != null && actFileTs.before(earliestTs)) {
                    // 현재 그룹 이름을 파일 이름으로 변경
                    groupActivities.forEach(a -> updateFileGroup(a.getId(), actFileName, actFileType));
                    updateFileGroup(activity.getId(), actFileName, actFileType);
                } else {
                    // 그룹 이름을 업데이트하지 않음
                    updateFileGroup(activity.getId(), groupName, actFileType);
                }
                groupUpdated = true;
                break;
            }
        }

        // 조건에 맞는 그룹이 없거나 그룹 이름을 업데이트하지 않은 경우, 현재 파일의 그룹을 설정
        if (!groupUpdated) {
            updateFileGroup(activity.getId(), actFileName, actFileType);
        }
    }

    private void updateFileGroup(long activityId, String groupName, String groupType) {
        // FileGroup 객체를 생성하여 데이터베이스에 저장
        FileGroup fileGroup = new FileGroup(activityId, groupName, groupType); // groupType 추가
        fileGroupRepo.save(fileGroup);
    }
}