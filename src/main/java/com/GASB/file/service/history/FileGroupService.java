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

    // 공통된 부분 추출 메서드
    private String getCommonSubstring(String a, String b) {
        int maxLength = Math.min(a.length(), b.length());
        for (int length = maxLength; length > 0; length--) {
            for (int start = 0; start <= a.length() - length; start++) {
                String substring = a.substring(start, start + length);
                if (b.contains(substring)) {
                    return substring;
                }
            }
        }
        return "";  // No common substring found
    }

    // 확장자를 제거한 파일 이름을 반환하는 메서드
    private String getFileNameWithoutExtension(String fileName) {
        return FilenameUtils.getBaseName(fileName).toLowerCase();  // Convert to lower case for consistency
    }

    // 파일을 동기적으로 처리하는 메서드
    public void groupFilesAndSave(long actId) {

        // activities 객체, by actId
        // 1. 파일 ID로 Activities 객체 조회
        Activities activity = activitiesRepo.findById(actId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
        // 2. monitored_users조회
        MonitoredUsers monitoredUsers = activity.getUser();
        // 3. orgSaas 조회
        OrgSaaS orgSaaS = monitoredUsers.getOrgSaaS();
        // 4. orgId 조회
        long orgId = orgSaaS.getOrg().getId();
//        Org org = orgSaaS.getOrg();
//        // 5. activities객체의 orgId
//        long orgId = org.getId();
        System.out.println("orgId: " + orgId);

        // activities 테이블의 모든 튜플 리스팅
        List<Activities> activitiesList = activitiesRepo.findAll();
        // actList에서 조건에 맞게 선정 -> selList로 추출
        List<Activities> selectedActivities = activitiesList.stream()
                .filter(a-> {
                    MonitoredUsers otherMonitoredUsers = a.getUser();
                    if(otherMonitoredUsers == null){
                        return false;
                    }
                    OrgSaaS otherOrgSaaS = otherMonitoredUsers.getOrgSaaS();
//                    Org otherOrg = otherOrgSaaS.getOrg();
                    return otherOrgSaaS != null && otherOrgSaaS.getOrg().getId() == orgId;
                })
                .collect(Collectors.toList());
        // List 출력
        System.out.println("Selected Activities:");

        // Debug the filter logic
        for (Activities a : selectedActivities) {
            MonitoredUsers otherMonitoredUsers = a.getUser();
            if (otherMonitoredUsers != null) {
                OrgSaaS otherOrgSaaS = otherMonitoredUsers.getOrgSaaS();
                if (otherOrgSaaS != null) {
                    long otherOrgId = otherOrgSaaS.getOrg().getId();
                    System.out.println("activityID: " + a.getId() + ", orgID: " + otherOrgId + ", eventTs:" + a.getEventTs());
                }
            }
        }

        // 확장자 제거한 파일네임
        String actFileName = getFileNameWithoutExtension(activity.getFileName());
        Timestamp actFileTs = Timestamp.valueOf(activity.getEventTs());

        // 9. 파일 이름 유사도 기반 그룹화 로직
        List<FileGroup> fileGroups = selectedActivities.stream()
                .map(a -> {
                    String otherFileName = getFileNameWithoutExtension(a.getFileName());
                    Timestamp otherFileTs = Timestamp.valueOf(a.getEventTs());
                    double similarity = calculateSimilarity(actFileName, otherFileName);

                    System.out.println("\nCompare: " + actFileName + " vs " + otherFileName);
                    System.out.println("similarity: " + similarity);

                    String groupName;

                    if (similarity >= SIM_THRESHOLD) {
                        // 유사도가 0.8 이상인 경우, ts가 빠른 파일의 이름을 그룹 이름으로 지정
                        if(actFileTs.compareTo(otherFileTs) <= 0) {
                            groupName = getCommonSubstring(actFileName, otherFileName);
                        } else {
                            groupName = getCommonSubstring(actFileName, otherFileName);
                        }
                    } else {
                        // 유사도가 0.8 이하인 경우, 파일 이름을 그룹 이름으로 설정
                        groupName = otherFileName;
                    }
                    System.out.println("groupName: " + groupName);

                    // FileGroup 객체 생성
                    return new FileGroup(a.getId(), groupName);
                })
                .collect(Collectors.toList());


//        // 10. 그룹 이름 결정
//        String groupName = fileGroups.stream()
//                .map(FileGroup::getGroupName)
//                .distinct()
//                .findFirst()
//                .orElse("Unknown Group");

        // 11. 결과를 file_group 테이블에 저장
        saveGroupsToFileGroupTable(fileGroups);
    }

    private void saveGroupsToFileGroupTable(List<FileGroup> fileGroups) {
        // 데이터베이스에 저장할 전처리 작업을 수행할 수 있음 (예: 기존 데이터 삭제 등)

        // 기존 데이터 삭제
        fileGroupRepo.deleteAll();

        // 파일 그룹 리스트를 데이터베이스에 저장
        fileGroups.forEach(fileGroup -> {
            fileGroupRepo.save(fileGroup);
        });
    }

}
