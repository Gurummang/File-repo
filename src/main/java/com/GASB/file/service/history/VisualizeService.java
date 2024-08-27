package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.FileHistoryBySaaS;
import com.GASB.file.model.dto.response.history.FileRelationEdges;
import com.GASB.file.model.dto.response.history.NodeAndSimilarity;
import com.GASB.file.model.entity.Activities;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileGroupRepo;
import com.GASB.file.repository.file.FileUploadRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class VisualizeService {

    private final ActivitiesRepo activitiesRepo;
    private final FileUploadRepo fileUploadRepo;
    private final FileGroupRepo fileGroupRepo;
    private final FileSimilar3Service fileSimilarService;

    public VisualizeService(ActivitiesRepo activitiesRepo, FileUploadRepo fileUploadRepo, FileGroupRepo fileGroupRepo, FileSimilar3Service fileSimilarService){
        this.activitiesRepo = activitiesRepo;
        this.fileUploadRepo = fileUploadRepo;
        this.fileGroupRepo = fileGroupRepo;
        this.fileSimilarService = fileSimilarService;
    }

    private Activities getActivity(long eventId) {
        return activitiesRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
    }

    public FileHistoryBySaaS getFileHistoryBySaaS(long eventId, long orgId) {

        Activities activity = getActivity(eventId);
        List<Activities> sameHashNodes = getHashList(activity, orgId);

        // 파일 히스토리 맵 초기화
        Set<Activities> nodes = new HashSet<>();
        List<FileRelationEdges> edges = new ArrayList<>();

        for (Activities a : sameHashNodes) {;
            edgesWithSaasFileId(a, nodes, edges);
        }

        edgesWithHash(nodes, edges);
        getGroupList(nodes, edges, eventId, orgId);

        List<FileRelationEdges> uniqueEdges = removeDuplicateEdges(edges);


        try {
            // 파일 유사도 계산 및 결과 가져오기
            NodeAndSimilarity nodesList = fileSimilarService.getFileSimilarity(activity, nodes);

            return FileHistoryBySaaS.builder()
                    .originNode(eventId)
                    .slack(nodesList.getSlackNodes())
                    .googleDrive(nodesList.getGoogleDriveNodes())
                    .o365(nodesList.getO365Nodes())
                    .edges(uniqueEdges) // 필터링된 엣지 리스트
                    .build();
        } catch (IOException | TikaException e) {
            log.error("Error calculating file similarity", e);

            // 예외 발생 시 빈 리스트를 사용하여 반환
            return FileHistoryBySaaS.builder()
                    .originNode(eventId)
                    .slack(new ArrayList<>()) // 빈 Slack 리스트
                    .googleDrive(new ArrayList<>()) // 빈 Google Drive 리스트
                    .o365(new ArrayList<>())
                    .edges(uniqueEdges) // 필터링된 엣지 리스트
                    .build();
        }
    }


    private List<Activities> getHashList(Activities activity, long orgId) {
        // 원본 해시값을 가져옴
        String originHash = fileUploadRepo.findHashByOrgSaaS_IdAndSaasFileId(
                activity.getUser().getOrgSaaS().getId(),
                activity.getSaasFileId(),
                activity.getEventTs()
        );

        if (!"file_delete".equals(activity.getEventType())) {
            // 동일한 해시값을 가진 Activities 리스트 가져오기
            return activitiesRepo.findByHash(originHash, orgId);
        } else{
            List<Activities> result = new ArrayList<>();
            result.add(activity);
            return result;
        }
    }

    private void edgesWithSaasFileId(Activities a, Set<Activities> nodes, List<FileRelationEdges> edges){
        String saasFileId = a.getSaasFileId();
        int orgSaasId = a.getUser().getOrgSaaS().getId();
        List<Activities> nodeList = activitiesRepo.findListBySaasFileId(saasFileId, orgSaasId);
        nodeList.sort(Comparator.comparing(Activities::getEventTs));
        nodes.addAll(nodeList);
        for (int i = 0; i < nodeList.size() - 1; i++) {
            Activities current = nodeList.get(i);
            Activities next = nodeList.get(i + 1);

            // 새로운 엣지 생성
            FileRelationEdges edge = new FileRelationEdges(
                    current.getId(), // 현재 노드 ID
                    next.getId(),    // 다음 노드 ID
                    "File_SaaS_Match"       // 엣지 타입
            );

            // 엣지 리스트에 추가
            edges.add(edge);
        }
    }

    private void edgesWithHash(Set<Activities> nodes, List<FileRelationEdges> edges) {
        Map<String, List<Activities>> hashMap = new HashMap<>();

        for (Activities node : nodes) {
            String hashValue = fileUploadRepo.findHashByOrgSaaS_IdAndSaasFileId(
                    node.getUser().getOrgSaaS().getId(),
                    node.getSaasFileId(),
                    node.getEventTs()
            );

            // hashMap에 해시값을 키로 그룹화
            hashMap.computeIfAbsent(hashValue, k -> new ArrayList<>()).add(node);
        }

        // 각 해시 그룹에서 시간 순으로 노드를 연결하여 엣지 생성
        for (List<Activities> nodeGroup : hashMap.values()) {
            nodeGroup.sort(Comparator.comparing(Activities::getEventTs));
            nodes.addAll(nodeGroup);
            for (int i = 0; i < nodeGroup.size() - 1; i++) {
                Activities current = nodeGroup.get(i);
                Activities next = nodeGroup.get(i + 1);

                FileRelationEdges edge = new FileRelationEdges(
                        current.getId(), // 현재 노드 ID
                        next.getId(),    // 다음 노드 ID
                        "File_Hash_Match"     // 엣지 타입
                );

                edges.add(edge);
            }
        }
    }

    private void getGroupList(Set<Activities> nodes, List<FileRelationEdges> edges, long eventId, long orgId){
        String groupName = fileGroupRepo.findGroupNameById(eventId);
        log.info(groupName);
        log.info("orgID: {}", orgId);
        // 동일한 그룹에 속하는 활동들을 가져옴
        List<Activities> sameGroups = activitiesRepo.findByOrgIdAndGroupName(orgId, groupName);
        nodes.addAll(sameGroups);
        sameGroups.sort(Comparator.comparing(Activities::getEventTs));
        for (int i = 0; i < sameGroups.size() - 1; i++) {
            Activities current = sameGroups.get(i);
            Activities next = sameGroups.get(i + 1);

            // 새로운 엣지 생성
            FileRelationEdges edge = new FileRelationEdges(
                    current.getId(), // 현재 노드 ID
                    next.getId(),    // 다음 노드 ID
                    "File_Group_Relation"       // 엣지 타입
            );

            // 엣지 리스트에 추가
            edges.add(edge);
        }
    }

    private List<FileRelationEdges> removeDuplicateEdges(List<FileRelationEdges> edges) {
        // 엣지 중복 제거를 위한 Map
        Map<String, FileRelationEdges> edgeMap = new HashMap<>();
        for (FileRelationEdges edge : edges) {
            String key = edge.getSource() + "-" + edge.getTarget();
            if (!edgeMap.containsKey(key)) {
                edgeMap.put(key, edge);
            } else {
                // 현재 엣지의 타입이 더 높은 우선순위를 가진 경우 교체
                FileRelationEdges existingEdge = edgeMap.get(key);
                if (getPriority(edge.getLabel()) < getPriority(existingEdge.getLabel())) {
                    edgeMap.put(key, edge);
                }
            }
        }

        // 최종적으로 중복이 제거된 엣지 리스트 반환
        return new ArrayList<>(edgeMap.values());
    }

    private int getPriority(String label) {
        return switch (label) {
            case "File_SaaS_Match" -> 1;
            case "File_Hash_Match" -> 2;
            case "File_Group_Relation"->3;
            default -> Integer.MAX_VALUE; // Unknown label
        };
    }
}
