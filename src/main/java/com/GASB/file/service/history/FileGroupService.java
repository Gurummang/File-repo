package com.GASB.file.service.history;

import com.GASB.file.model.entity.*;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileGroupRepo;
import com.GASB.file.repository.org.OrgSaaSRepo;
import com.GASB.file.repository.user.SlackUserRepo;
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
    private final OrgSaaSRepo orgSaaSRepo;
    private final SlackUserRepo slackUserRepo;

    @Autowired
    public FileGroupService(ActivitiesRepo activitiesRepo, FileGroupRepo fileGroupRepo, OrgSaaSRepo orgSaaSRepo, SlackUserRepo slackUserRepo) {
        this.activitiesRepo = activitiesRepo;
        this.fileGroupRepo = fileGroupRepo;
        this.orgSaaSRepo = orgSaaSRepo;
        this.slackUserRepo = slackUserRepo;
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

    public void groupFilesAndSave(long actId) {
        // 1. 파일 ID로 Activities 객체 조회
        Activities activity = activitiesRepo.findById(actId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // 2. monitored_users 조회
        MonitoredUsers monitoredUsers = activity.getUser();

        // 3. orgSaaS 조회
        OrgSaaS orgSaaS = monitoredUsers.getOrgSaaS();

        // 4. orgId 조회
        long orgId = orgSaaS.getOrg().getId();
        System.out.println("orgId: " + orgId);

        // activities 테이블의 모든 튜플 리스팅
        List<Activities> activitiesList = activitiesRepo.findAll();

        // 중복 제외한 selectedActivities 리스트 생성
        List<Activities> selectedActivities = activitiesList.stream()
                .filter(a -> a.getId() != actId) // 입력받은 actId와 다른 항목만 선택
                .filter(a -> {
                    MonitoredUsers otherMonitoredUsers = a.getUser();
                    return otherMonitoredUsers != null && otherMonitoredUsers.getOrgSaaS().getOrg().getId() == orgId;
                })
                .distinct() // 중복 제거
                .toList();

        System.out.println("Selected Activities:");
        selectedActivities.forEach(a -> System.out.println("Activity ID: " + a.getId() + ", File Name: " + a.getFileName() + ", Event Timestamp: " + a.getEventTs()));

        // 2. 그룹 이름 추출 및 null과 중복 제거
        Set<String> groupNames = selectedActivities.stream()
                .map(a -> fileGroupRepo.findGroupNameById(a.getId())) // groupName 조회
                .filter(Objects::nonNull) // null 제거
                .collect(Collectors.toSet()); // 중복 제거

        System.out.println("Group Names:");
        groupNames.forEach(name -> System.out.println("Group Name: " + name));

        // 3. 현재 검사 주체의 파일 이름
        String actFileName = getFileNameWithoutExtension(activity.getFileName());
        Timestamp actFileTs = Timestamp.valueOf(activity.getEventTs());

        System.out.println("Current File Name: " + actFileName);
        System.out.println("Current File Timestamp: " + actFileTs);

        // 4. 유사도 0.8 이상인 그룹의 이름을 찾고 그룹의 파일을 업데이트
        boolean groupUpdated = false;
        for (String groupName : groupNames) {
            double similarity = calculateSimilarity(actFileName, groupName);
            System.out.println("Comparing with Group Name: " + groupName + ", Similarity: " + similarity);

            if (similarity >= SIM_THRESHOLD) {
                // 그룹의 파일들 중 가장 빠른 타임스탬프 찾기
                List<Activities> groupActivities = selectedActivities.stream()
                        .filter(a -> groupName.equals(fileGroupRepo.findGroupNameById(a.getId())))
                        .toList();

                Timestamp earliestTs = groupActivities.stream()
                        .map(a -> Timestamp.valueOf(a.getEventTs()))
                        .min(Comparator.naturalOrder())
                        .orElse(null);

                System.out.println("Group Name: " + groupName + ", Earliest Timestamp: " + earliestTs);

                if (earliestTs != null && actFileTs.before(earliestTs)) {
                    System.out.println("Current File Timestamp is earlier than the earliest timestamp of the group.");

                    // 현재 그룹 이름을 파일 이름으로 변경
                    groupActivities.forEach(a -> updateFileGroup(a.getId(), actFileName));
                    updateFileGroup(activity.getId(), actFileName);

                    System.out.println("Group name updated to: " + actFileName);
                    groupUpdated = true;
                    break; // 그룹이 업데이트 되었으므로 추가 비교 필요 없음
                } else {
                    // 그룹 이름을 업데이트하지 않음
                    updateFileGroup(activity.getId(), groupName);
                    System.out.println("File grouped under existing group: " + groupName);
                    groupUpdated = true;
                    break;
                }
            }
        }

        // 조건에 맞는 그룹이 없거나 그룹 이름을 업데이트하지 않은 경우, 현재 파일의 그룹을 설정
        if (!groupUpdated) {
            System.out.println("No similar group found or no timestamp update required.");
            updateFileGroup(activity.getId(), actFileName);
        }
    }

    private void updateFileGroup(long activityId, String groupName) {
        // FileGroup 객체를 생성하여 데이터베이스에 저장
        FileGroup fileGroup = new FileGroup(activityId, groupName);
        fileGroupRepo.save(fileGroup);

        // 디버깅 출력
        System.out.println("FileGroup saved: Activity ID = " + activityId + ", Group Name = " + groupName);
    }
}
