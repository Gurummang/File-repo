package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.*;
import com.GASB.file.model.entity.Activities;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileGroupRepo;
import com.GASB.file.repository.file.FileUploadRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileVisualizeService {

    private final ActivitiesRepo activitiesRepo;
    private final FileUploadRepo fileUploadRepo;
    private final FileGroupRepo fileGroupRepo;
    private final FileSimilarService fileSimilarService;

    public FileVisualizeService(ActivitiesRepo activitiesRepo, FileUploadRepo fileUploadRepo, FileGroupRepo fileGroupRepo, FileSimilarService fileSimilarService) {
        this.activitiesRepo = activitiesRepo;
        this.fileUploadRepo = fileUploadRepo;
        this.fileGroupRepo = fileGroupRepo;
        this.fileSimilarService = fileSimilarService;
    }

    private List<FileRelationEdges> filterTransitiveEdges(List<FileRelationEdges> edges) {
        // Maps to track adjacency and labels
        Map<Long, Set<Long>> adjacencyMap = new HashMap<>();
        Map<String, Set<String>> edgeLabelsMap = new HashMap<>();

        // Build adjacency and label maps
        for (FileRelationEdges edge : edges) {
            adjacencyMap
                    .computeIfAbsent(edge.getSource(), k -> new HashSet<>())
                    .add(edge.getTarget());
            edgeLabelsMap.computeIfAbsent(edge.getSource() + "-" + edge.getTarget(), k -> new HashSet<>()).add(edge.getLabel());
        }

        // Set to track filtered edges
        Set<String> filteredEdges = new HashSet<>();

        // Add only non-transitive edges
        for (FileRelationEdges edge : edges) {
            if (!isTransitive(edge.getSource(), edge.getTarget(), adjacencyMap)) {
                filteredEdges.add(edge.getSource() + "-" + edge.getTarget());
            }
        }

        // Construct final list of edges
        return filteredEdges.stream()
                .map(edgeKey -> {
                    String[] parts = edgeKey.split("-");
                    // Retrieve the appropriate label from edgeLabelsMap (assuming one label per edge)
                    String label = edgeLabelsMap.get(edgeKey).iterator().next();
                    return new FileRelationEdges(Long.parseLong(parts[0]), Long.parseLong(parts[1]), label);
                })
                .collect(Collectors.toList());
    }

    private boolean isTransitive(Long source, Long target, Map<Long, Set<Long>> adjacencyMap) {
        if (!adjacencyMap.containsKey(source)) return false;

        Set<Long> directTargets = adjacencyMap.get(source);
        for (Long intermediate : directTargets) {
            if (adjacencyMap.containsKey(intermediate) && adjacencyMap.get(intermediate).contains(target)) {
                return true; // Transitive relationship found
            }
        }
        return false;
    }

    public FileHistoryBySaaS getFileHistoryBySaaS(long eventId) {
        Activities activity = getActivity(eventId);
        Activities startActivity = activitiesRepo.getActivitiesBySaaSFileId(activity.getSaasFileId());

        Map<String, List<FileRelationNodes>> fileHistoryMap = initializeFileHistoryMap();
        Map<Long, FileRelationNodes> nodesMap = new HashMap<>();
        List<FileRelationEdges> edges = new ArrayList<>();
        Set<Long> seenEventIds = new HashSet<>();

        exploreFileRelationsDFS(startActivity, 2, seenEventIds, nodesMap, edges);

        String saasName = getSaasName(startActivity);
        List<FileRelationNodes> nodesList = new ArrayList<>(nodesMap.values());
        populateFileHistoryMap(fileHistoryMap, saasName, nodesList);

        // Filter out transitive edges
        List<FileRelationEdges> filteredEdges = filterTransitiveEdges(edges);

        return FileHistoryBySaaS.builder()
                .slack(fileHistoryMap.get("slack"))
                .googleDrive(fileHistoryMap.get("googleDrive"))
                .edges(filteredEdges)
                .build();
    }


    private Activities getActivity(long eventId) {
        return activitiesRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
    }

    private Map<String, List<FileRelationNodes>> initializeFileHistoryMap() {
        Map<String, List<FileRelationNodes>> fileHistoryMap = new HashMap<>();
        fileHistoryMap.put("slack", new ArrayList<>());
        fileHistoryMap.put("googleDrive", new ArrayList<>());
        return fileHistoryMap;
    }

    private String getSaasName(Activities activity) {
        return activity.getUser().getOrgSaaS().getSaas().getSaasName().toLowerCase();
    }

    private void populateFileHistoryMap(Map<String, List<FileRelationNodes>> fileHistoryMap, String saasName, List<FileRelationNodes> nodesList) {
        if ("slack".equals(saasName)) {
            fileHistoryMap.get("slack").addAll(nodesList);
        } else if ("googledrive".equals(saasName)) {
            fileHistoryMap.get("googleDrive").addAll(nodesList);
        }
    }

    private void exploreFileRelationsDFS(Activities startActivity, int maxDepth, Set<Long> seenEventIds, Map<Long, FileRelationNodes> nodesMap, List<FileRelationEdges> edges) {
        if (maxDepth < 0) return;

        // 현재 활동 처리
        processCurrentActivity(startActivity, seenEventIds, nodesMap);

        // SaaSFileID로 일치하는 활동 목록
        List<Activities> sameSaasFiles = activitiesRepo.findListBySaasFileId(startActivity.getSaasFileId());
        System.out.println("sameSaasFiles:");
        sameSaasFiles.forEach(activity -> System.out.println(activity.getId()));
        // Hash로 일치하는 활동 목록 중 eventType이 'file_uploaded'인 것들만 필터링
        List<Activities> sameHashFiles = activitiesRepo.findByHash(getSaltedHash(startActivity));

        // sameHashFiles 리스트 출력
        System.out.println("sameHashFiles:");
        sameHashFiles.forEach(activity -> System.out.println(activity.getId()));  // 활동 객체의 toString() 메서드를 통해 상세 정보를 출력

        // SaaSFileID가 일치하는 활동을 sameHashFiles에서 제거
        sameHashFiles.removeAll(sameSaasFiles);
        System.out.println("removeAllsameHashFiles:");
        sameHashFiles.forEach(activity -> System.out.println(activity.getId()));

        // SaaSFileID로 일치하는 활동들에 대해 연결 추가
        addRelatedActivities(sameSaasFiles, startActivity, seenEventIds, nodesMap, edges, "File_SaaS_Match", maxDepth);

        // Hash로 일치하는 활동들에 대해 연결 추가
        addRelatedActivities(sameHashFiles, startActivity, seenEventIds, nodesMap, edges, "File_Hash_Match", maxDepth);
    }




    private void addRelatedActivities(List<Activities> relatedActivities, Activities startActivity, Set<Long> seenEventIds, Map<Long, FileRelationNodes> nodesMap, List<FileRelationEdges> edges, String edgeType, int currentDepth) {
        for (Activities relatedActivity : relatedActivities) {
            if (!seenEventIds.contains(relatedActivity.getId()) && !relatedActivity.getId().equals(startActivity.getId())) {
                FileRelationNodes targetNode = createFileRelationNodes(relatedActivity);
                nodesMap.putIfAbsent(relatedActivity.getId(), targetNode);

                // 엣지 추가
                edges.add(new FileRelationEdges(startActivity.getId(), relatedActivity.getId(), edgeType));

                // DFS 탐색 계속 진행 (depth 감소)
                if (currentDepth > 0) {
                    exploreFileRelationsDFS(relatedActivity, currentDepth - 1, seenEventIds, nodesMap, edges);
                }
            }
        }
    }


    private void processCurrentActivity(Activities activity, Set<Long> seenEventIds, Map<Long, FileRelationNodes> nodesMap) {
        seenEventIds.add(activity.getId());
        FileRelationNodes node = createFileRelationNodes(activity);
        nodesMap.putIfAbsent(activity.getId(), node);
    }



    private FileRelationNodes createFileRelationNodes(Activities activity) {
        double similarity = fileSimilarService.getFileSimilarity(activity.getId(), activity.getId());
        BigDecimal roundedSimilarity = new BigDecimal(similarity).setScale(2, RoundingMode.HALF_UP);
        return FileRelationNodes.builder()
                .eventId(activity.getId())
                .saas(activity.getUser().getOrgSaaS().getSaas().getSaasName())
                .eventType(activity.getEventType())
                .fileName(activity.getFileName())
                .hash256(getSaltedHash(activity))
                .saasFileId(activity.getSaasFileId())
                .eventTs(activity.getEventTs())
                .email(activity.getUser().getEmail())
                .uploadChannel(activity.getUploadChannel())
                .similarity(roundedSimilarity.doubleValue())
                .build();
    }

    private String getSaltedHash(Activities activity) {
        return fileUploadRepo.findHashByOrgSaaS_IdAndSaasFileId(activity.getUser().getOrgSaaS().getId(), activity.getSaasFileId(), activity.getEventTs());
    }
}
