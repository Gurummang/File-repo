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
        return FilenameUtils.getBaseName(fileName);
    }

    // 파일 그룹화 및 저장
    public void groupFilesAndSave() {
        // 파일 이름과 생성 시간을 조회
        List<Activities> activitiesList = activitiesRepo.findAll();
        List<String> fileNames = activitiesList.stream()
                .map(Activities::getFileName)
                .distinct()
                .collect(Collectors.toList());

        // 그룹화 로직
        Map<String, List<Activities>> groupedFiles = new HashMap<>();
        Set<String> processedFiles = new HashSet<>();

        for (int i = 0; i < fileNames.size(); i++) {
            String fileName1 = getFileNameWithoutExtension(fileNames.get(i));
            if (processedFiles.contains(fileName1)) continue;

            List<Activities> group = new ArrayList<>();
            group.addAll(activitiesList.stream()
                    .filter(activity -> getFileNameWithoutExtension(activity.getFileName()).equals(fileName1))
                    .collect(Collectors.toList()));

            processedFiles.add(fileName1);

            for (int j = i + 1; j < fileNames.size(); j++) {
                String fileName2 = getFileNameWithoutExtension(fileNames.get(j));
                if (calculateSimilarity(fileName1, fileName2) > SIM_THRESHOLD) {
                    group.addAll(activitiesList.stream()
                            .filter(activity -> getFileNameWithoutExtension(activity.getFileName()).equals(fileName2))
                            .collect(Collectors.toList()));
                    processedFiles.add(fileName2);
                }
            }

            String groupName = group.stream()
                    .map(activity -> getFileNameWithoutExtension(activity.getFileName()))
                    .reduce((a, b) -> getCommonSubstring(a, b))
                    .orElse("Unknown Group");

            groupedFiles.put(groupName, group);
        }

        // 결과를 file_group 테이블에 저장
        saveGroupsToFileGroupTable(groupedFiles);
    }

    // 데이터베이스에서 그룹화된 파일 정보 조회
    public List<Map<String, Object>> fileGrouping() {
        // file_group 테이블에서 모든 그룹 정보를 조회
        List<FileGroup> fileGroups = fileGroupRepo.findAll();

        // file_group과 activities 테이블을 조인하여 그룹화된 파일 정보를 생성
        Map<String, List<FileHistoryCorrelation>> groupedFiles = fileGroups.stream()
                .collect(Collectors.groupingBy(
                        FileGroup::getGroupName,
                        Collectors.mapping(
                                fileGroup -> {
                                    // activities 테이블에서 파일 정보를 조회
                                    Activities activity = activitiesRepo.findById(fileGroup.getId()).orElse(null);
                                    if (activity != null) {
                                        // FileHistoryCorrelation 객체 생성
                                        return FileHistoryCorrelation.builder()
                                                .eventId(activity.getId())
                                                .saas("SampleSaaS") // 필요시 실제 값을 넣으세요
                                                .eventType(activity.getEventType())
                                                .fileName(activity.getFileName())
                                                .saasFileId(activity.getSaasFileId())
                                                .eventTs(activity.getEventTs())
                                                .email("SampleEmail") // 필요시 실제 값을 넣으세요
                                                .uploadChannel(activity.getUploadChannel())
                                                .build();
                                    }
                                    return null;
                                },
                                Collectors.toList()
                        )
                ));

        // 결과를 List<Map<String, Object>> 형태로 변환하여 반환
        return groupedFiles.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("group", entry.getKey());
                    result.put("files", entry.getValue()); // FileHistoryCorrelation 객체 리스트를 포함

                    // 그룹에서 가장 빠른 파일을 Origin으로 설정
                    FileHistoryCorrelation originFile = entry.getValue().stream()
                            .min(Comparator.comparing(FileHistoryCorrelation::getEventTs))
                            .orElse(null);

//                    result.put("origin", originFile != null ? originFile.getFileName() : "No Origin");
                    return result;
                })
                .collect(Collectors.toList());
    }


    private void saveGroupsToFileGroupTable(Map<String, List<Activities>> groupedFiles) {
        // 이전 데이터 삭제 (옵션)
        fileGroupRepo.deleteAll();

        // 새로운 그룹 데이터 저장
        for (Map.Entry<String, List<Activities>> entry : groupedFiles.entrySet()) {
            String groupName = entry.getKey();
            List<Activities> activitiesInGroup = entry.getValue();

            for (Activities activity : activitiesInGroup) {
                FileGroup fileGroup = new FileGroup();
                fileGroup.setId(activity.getId()); // file_group 테이블의 id를 activities 테이블의 id로 설정
                fileGroup.setGroupName(groupName);
                fileGroupRepo.save(fileGroup);
            }
        }
    }

    public List<FileHistoryCorrelation> getFileGroupingData() {
        // 데이터베이스에서 그룹화된 파일 정보 조회
        List<FileGroup> fileGroups = fileGroupRepo.findAll();

        // file_group과 activities 테이블을 조인하여 그룹화된 파일 정보를 생성
        Map<String, List<FileHistoryCorrelation>> groupedFiles = fileGroups.stream()
                .collect(Collectors.groupingBy(
                        FileGroup::getGroupName,
                        Collectors.mapping(
                                fileGroup -> {
                                    // activities 테이블에서 파일 정보를 조회
                                    Activities activity = activitiesRepo.findById(fileGroup.getId()).orElse(null);
                                    if (activity != null) {
                                        // FileHistoryCorrelation 객체 생성
                                        return FileHistoryCorrelation.builder()
                                                .eventId(activity.getId())
                                                .saas("SampleSaaS") // 필요시 실제 값을 넣으세요
                                                .eventType(activity.getEventType())
                                                .fileName(activity.getFileName())
                                                .saasFileId(activity.getSaasFileId())
                                                .eventTs(activity.getEventTs())
                                                .email("SampleEmail") // 필요시 실제 값을 넣으세요
                                                .uploadChannel(activity.getUploadChannel())
                                                .build();
                                    }
                                    return null;
                                },
                                Collectors.toList()
                        )
                ));

        // 결과를 List<FileHistoryCorrelation> 형태로 변환하여 반환
        return groupedFiles.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());
    }
}
