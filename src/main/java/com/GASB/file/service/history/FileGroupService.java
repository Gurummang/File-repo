package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.FileHistoryCorrelation;
import com.GASB.file.model.entity.Activities;
import com.GASB.file.model.entity.FileGroup;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileGroupRepo;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FileGroupService {

    private static final double SIM_THRESHOLD = 0.7;

    @Autowired
    private ActivitiesRepo activitiesRepo;

    @Autowired
    private FileGroupRepo fileGroupRepo;

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

        // 파일 ID로 Activities 객체 조회
        Activities activity = activitiesRepo.findById(actId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        // 파일 이름을 포함한 Activities 객체 목록 가져오기
        List<Activities> activitiesList = activitiesRepo.findAll();
        String fileName1 = getFileNameWithoutExtension(activity.getFileName());

        // 그룹화 로직
        List<Activities> group = activitiesList.stream()
                .filter(a -> getFileNameWithoutExtension(a.getFileName()).equals(fileName1))
                .collect(Collectors.toList());

        // 파일 이름 유사성 기반 그룹화
        for (Activities otherActivity : activitiesList) {
            String fileName2 = getFileNameWithoutExtension(otherActivity.getFileName());
            if (calculateSimilarity(fileName1, fileName2) > SIM_THRESHOLD) {
                group.add(otherActivity);
            }
        }

        // 그룹 이름 결정
        String groupName = group.stream()
                .map(a -> getFileNameWithoutExtension(a.getFileName()))
                .reduce((a, b) -> getCommonSubstring(a, b))
                .orElse("Unknown Group");

        // 결과를 file_group 테이블에 저장
        saveGroupsToFileGroupTable(groupName, group);
    }

    private void saveGroupsToFileGroupTable(String groupName, List<Activities> activitiesInGroup) {
        fileGroupRepo.deleteAll(); // Consider using batch operations for large data

        for (Activities activity : activitiesInGroup) {
            FileGroup fileGroup = new FileGroup();
            fileGroup.setId(activity.getId());
            fileGroup.setGroupName(groupName);
            fileGroupRepo.save(fileGroup);
        }
    }
}
