package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.*;
import com.GASB.file.model.entity.Activities;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileGroupRepo;
import com.GASB.file.repository.file.FileUploadRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileVisualizeService {

    private final ActivitiesRepo activitiesRepo;
    private final FileUploadRepo fileUploadRepo;
    private final FileGroupRepo fileGroupRepo;
    private final FileSimilar3Service fileSimilarService;
    private static final String FILE_UPLOAD = "file_upload";

    public FileVisualizeService(ActivitiesRepo activitiesRepo, FileUploadRepo fileUploadRepo, FileGroupRepo fileGroupRepo, FileSimilar3Service fileSimilarService) {
        this.activitiesRepo = activitiesRepo;
        this.fileUploadRepo = fileUploadRepo;
        this.fileGroupRepo = fileGroupRepo;
        this.fileSimilarService = fileSimilarService;
    }

    // a->b, b->c가 만족할 때, 굳이 a->c로 연결하지 않음.
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
                .toList();
    }

    // 특정 엣지가 전이적인지 확인
    // 출발지(source)에서 도착지(target)까지의 간접 경로가 존재하는지 확인하여, 전이적인 경우 true를 반환하고, 그렇지 않으면 false를 반환
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

    //활동(Activity) 데이터를 가져오고 시작 활동을 기준으로 히스토리를 추적합니다.
    //파일 히스토리 맵을 초기화하고, 노드와 엣지를 생성합니다.
    //DFS를 통해 파일 간의 관계를 탐색하고, 노드 및 엣지 정보를 갱신합니다.
    //필요한 정보를 필터링한 후 최종적으로 Slack과 Google Drive에 해당하는 파일 히스토리 및 엣지 데이터를 반환합니다.
    public FileHistoryBySaaS getFileHistoryBySaaS(long eventId, long orgId) {
        // 활동(Activity) 데이터를 가져옵니다.
        Activities activity = getActivity(eventId);
        Activities startActivity = activitiesRepo.getActivitiesBySaaSFileId(activity.getSaasFileId());

        // 파일 히스토리 맵 초기화
        Set<Activities> nodes = new HashSet<>();
        List<FileRelationEdges> edges = new ArrayList<>();
        Set<Long> seenEventIds = new HashSet<>();

        // DFS를 통해 파일 간의 관계를 탐색합니다.
        exploreFileRelationsDFS(startActivity, 2, seenEventIds, nodes, edges, eventId, orgId);
        addGroupRelatedActivities(eventId, seenEventIds, nodes, edges, orgId);

        log.info("Added Nodes: {}", nodes);

        List<FileRelationEdges> filteredEdges = filterTransitiveEdges(edges);

        try {
            // 파일 유사도 계산 및 결과 가져오기
            NodeAndSimilarity nodesList = fileSimilarService.getFileSimilarity(activity, nodes);

            return FileHistoryBySaaS.builder()
                    .originNode(eventId)
                    .slack(nodesList.getSlackNodes()) // Slack 노드 리스트
                    .googleDrive(nodesList.getGoogleDriveNodes()) // Google Drive 노드 리스트
                    .edges(filteredEdges) // 필터링된 엣지 리스트
                    .build();
        } catch (IOException | TikaException e) {
            log.error("Error calculating file similarity", e);

            // 예외 발생 시 빈 리스트를 사용하여 반환
            return FileHistoryBySaaS.builder()
                    .originNode(eventId)
                    .slack(new ArrayList<>()) // 빈 Slack 리스트
                    .googleDrive(new ArrayList<>()) // 빈 Google Drive 리스트
                    .edges(filteredEdges) // 필터링된 엣지 리스트
                    .build();
        }
    }


    private void removeEventIdFromSeen(Set<Long> seenEventIds, long eventId) {
        // eventId가 seenEventIds에 존재하는 경우에만 제거
        if (seenEventIds.contains(eventId)) {
            seenEventIds.remove(eventId);
            log.info("Removed eventId {} from seenEventIds", eventId);
        } else {
            log.info("eventId {} not found in seenEventIds", eventId);
        }
    }

    private void addGroupRelatedActivities(long eventId, Set<Long> seenEventIds, Set<Activities> nodes, List<FileRelationEdges> edges, long orgId) {
        log.info("-----------------hi!!!!----------------");
        // 그룹 이름을 가져옴
        String groupName = fileGroupRepo.findGroupNameById(eventId);
        log.info(groupName);
        log.info("orgID: {}", orgId);
        // 동일한 그룹에 속하는 활동들을 가져옴
        List<Activities> sameGroups = activitiesRepo.findByOrgIdAndGroupName(orgId, groupName);

        // 활동들을 이벤트 발생 시간 기준으로 오름차순 정렬
        sameGroups.sort(Comparator.comparing(Activities::getEventTs));

        removeEventIdFromSeen(seenEventIds, eventId);
        // 이전 활동 ID를 추적하기 위한 변수
        Long previousActivityId = null;

        // 모든 활동을 처리
        for (Activities a : sameGroups) {
            if (!seenEventIds.contains(a.getId())) {
                nodes.add(a);

                // 이전 활동과 현재 활동을 엣지로 연결
                if (previousActivityId != null) {
                    edges.add(new FileRelationEdges(previousActivityId, a.getId(), "File_Group_Relation"));
                    log.info("Added edge: {} -> {}", previousActivityId, a.getId());
                } else {
                    log.info("Starting with activity ID: {}", a.getId());
                }

                // 현재 활동을 처리된 것으로 표시
                seenEventIds.add(a.getId());
                log.info("seenIds: " + seenEventIds);

                // 이전 활동 ID 업데이트
                previousActivityId = a.getId();
            }
        }

        // 마지막 활동과 이전 활동 연결 (마지막으로 처리된 활동과 이전 활동)
        if (previousActivityId != null) {
            for (Activities a : sameGroups) {
                if (!seenEventIds.contains(a.getId()) && !a.getId().equals(previousActivityId)) {
                    edges.add(new FileRelationEdges(previousActivityId, a.getId(), "File_Group_Relation"));
                    log.info("Added edge: {} -> {}", previousActivityId, a.getId());
                    break;
                }
            }
        }
    }

    private Activities getActivity(long eventId) {
        return activitiesRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
    }

    //현재 활동을 처리하고, 이미 탐색된 활동을 추적하여 중복을 방지합니다.
    //SaaSFileID와 해시 값이 일치하는 활동들을 탐색하여 엣지를 추가합니다.
    //재귀적으로 깊이를 줄여가며 탐색을 진행합니다.
    private void exploreFileRelationsDFS(Activities startActivity, int maxDepth, Set<Long> seenEventIds, Set<Activities> nodes, List<FileRelationEdges> edges, long eventId, long orgId) {
        if (maxDepth < 0) return;

        // 현재 활동 처리
        log.info("시작 노드: {}", startActivity.getId());

        // 초기 활동 결정
        Activities initialActivity = determineInitialActivity(startActivity, seenEventIds);
        log.info("업로드 이벤트 노드(시작 노드 재설정): {}", initialActivity.getId());


        // SaaSFileID와 Hash로 일치하는 활동 목록 가져오기
        List<Activities> sameSaasFiles = findAndSortActivitiesBySaasFileId(initialActivity);
        List<Activities> sameHashFiles = findAndSortActivitiesByHash(initialActivity, orgId);
        log.info("SaaS파일id가 같음: " + sameSaasFiles.stream().map(Activities::getId).collect(Collectors.toList()));

        // sameHashFiles의 id 리스트 출력
        log.info("해시256값이 같음: " + sameHashFiles.stream().map(Activities::getId).collect(Collectors.toList()));

        // SaaSFileID로 일치하는 활동을 Hash 목록에서 제거
        removeDuplicateActivities(sameHashFiles, sameSaasFiles);

        // 새로운 초기 활동 설정
        Activities newInitialActivity = determineNewInitialActivity(startActivity, initialActivity, sameSaasFiles, sameHashFiles, seenEventIds);

        if (newInitialActivity != initialActivity) {
            // 새로운 초기 활동이 설정되면 해당 활동에 대한 정보 갱신
            sameSaasFiles = findAndSortActivitiesBySaasFileId(newInitialActivity);
            sameHashFiles = findAndSortActivitiesByHash(newInitialActivity, orgId);
            removeDuplicateActivities(sameHashFiles, sameSaasFiles);
        }

        // 연관된 활동들에 대한 처리
        processRelatedActivities(newInitialActivity, sameSaasFiles, sameHashFiles, seenEventIds, nodes, edges, maxDepth, eventId, orgId);
    }

    private Activities determineInitialActivity(Activities startActivity, Set<Long> seenEventIds) {
        Activities testActivity = activitiesRepo.getActivitiesBySaaSFileId(startActivity.getSaasFileId());
        if (!startActivity.getEventType().equals(FILE_UPLOAD) && !seenEventIds.contains(testActivity.getId())) {
            return testActivity;
        } else {
            return startActivity;
        }
    }

    private List<Activities> findAndSortActivitiesBySaasFileId(Activities activity) {
        return activitiesRepo.findListBySaasFileId(activity.getSaasFileId(), activity.getUser().getOrgSaaS().getId())
                .stream()
                .filter(a -> !a.getId().equals(activity.getId()))  // 초기 활동의 ID가 아닌 활동만 필터링
                .sorted(Comparator.comparing(Activities::getEventTs))  // 시간 순서로 정렬
                .collect(Collectors.toList());
    }

    private List<Activities> findAndSortActivitiesByHash(Activities activity, long orgId) {
        return activitiesRepo.findByHashAndOrgId(getSaltedHash(activity), orgId)
                .stream()
                .filter(a -> FILE_UPLOAD.equals(a.getEventType()))
                .sorted(Comparator.comparing(Activities::getEventTs))  // 시간 순서로 정렬
                .collect(Collectors.toList());
    }

    private void removeDuplicateActivities(List<Activities> sameHashFiles, List<Activities> sameSaasFiles) {
        sameHashFiles.removeAll(sameSaasFiles);
    }

    private Activities determineNewInitialActivity(Activities startActivity, Activities initialActivity, List<Activities> sameSaasFiles, List<Activities> sameHashFiles, Set<Long> seenEventIds) {
        if (sameSaasFiles.isEmpty() && !sameHashFiles.isEmpty() && !sameHashFiles.getFirst().getId().equals(startActivity.getId()) && !seenEventIds.contains(sameHashFiles.getFirst().getId())) {
            Activities newInitialActivity = sameHashFiles.getFirst();
            log.info("새로운 기준 노드 부여: {}", newInitialActivity.getId());
            return newInitialActivity;
        }
        return initialActivity;
    }

    private void processRelatedActivities(Activities initialActivity, List<Activities> sameSaasFiles, List<Activities> sameHashFiles, Set<Long> seenEventIds, Set<Activities> nodes, List<FileRelationEdges> edges, int maxDepth, long eventId, long orgId) {
        // SaaSFileID로 일치하는 활동들에 대해 연결 추가
        addRelatedActivities(sameSaasFiles, initialActivity, seenEventIds, nodes, edges, "File_SaaS_Match", maxDepth, eventId, orgId);

        // Hash로 일치하는 활동들에 대해 연결 추가
        addRelatedActivities(sameHashFiles, initialActivity, seenEventIds, nodes, edges, "File_Hash_Match", maxDepth, eventId, orgId);

        log.info("----------SaaSFileId랑 Hash값 둘다 봤음------------");
    }

    //주어진 활동들에 대해 노드를 생성하고, 엣지를 추가합니다.
    //재귀적으로 DFS 탐색을 진행하여 연결 관계를 계속해서 추적합니다.
    private void addRelatedActivities(List<Activities> relatedActivities, Activities startActivity, Set<Long> seenEventIds, Set<Activities> nodes, List<FileRelationEdges> edges, String edgeType, int currentDepth, long eventId, long orgId) {
        // 활동 리스트를 이벤트 발생 시간 기준으로 정렬 (오름차순)
        log.info("기준 노드: {}", startActivity.getId());
        processCurrentActivity(startActivity, seenEventIds, nodes);
        relatedActivities.sort(Comparator.comparing(Activities::getEventTs));
        log.info("seenEventIds에 들어갔냐? :{}", seenEventIds.contains(startActivity.getId()));
        for (Activities relatedActivity : relatedActivities) {
            log.info("기준 노드에서 탐색할 노드: {}", relatedActivity.getId());
            if (!seenEventIds.contains(relatedActivity.getId()) && !relatedActivity.getId().equals(startActivity.getId())) {
                nodes.add(relatedActivity);

                log.info("기준 , 탐색 : {}, {}", startActivity.getId(), relatedActivity.getId());
                // 엣지 추가 (startActivity와 시간 순으로 연결된 relatedActivity)
                edges.add(new FileRelationEdges(startActivity.getId(), relatedActivity.getId(), edgeType));

                // DFS 탐색 계속 진행 (depth 감소)
                if (currentDepth > 0) {
                    exploreFileRelationsDFS(relatedActivity, currentDepth - 1, seenEventIds, nodes, edges, eventId, orgId);
                }

                // 다음 연결을 위해 startActivity를 현재 relatedActivity로 업데이트
                if (relatedActivity.getEventType().equals(FILE_UPLOAD)) {
                    startActivity = relatedActivity;
                } else {
                    startActivity = activitiesRepo.getActivitiesBySaaSFileId(relatedActivity.getSaasFileId());
                }
                log.info("다음 노드:{}", startActivity.getId());
            }
        }
    }

    //활동을 처리하고 노드를 생성한 후, 해당 활동이 이미 처리되었음을 기록
    private void processCurrentActivity(Activities activity, Set<Long> seenEventIds, Set<Activities> nodes) {
        seenEventIds.add(activity.getId());
        nodes.add(activity);
    }

    private String getSaltedHash(Activities activity) {
        return fileUploadRepo.findHashByOrgSaaS_IdAndSaasFileId(activity.getUser().getOrgSaaS().getId(), activity.getSaasFileId(), activity.getEventTs());
    }
}
