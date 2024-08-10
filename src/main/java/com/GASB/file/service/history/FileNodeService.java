package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.*;
import com.GASB.file.model.entity.Activities;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileGroupRepo;
import com.GASB.file.repository.file.FileUploadRepo;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FileNodeService {

    private final ActivitiesRepo activitiesRepo;
    private final FileUploadRepo fileUploadRepo;
    private final FileGroupRepo fileGroupRepo;

    public FileNodeService(ActivitiesRepo activitiesRepo, FileUploadRepo fileUploadRepo, FileGroupRepo fileGroupRepo){
        this.activitiesRepo = activitiesRepo;
        this.fileUploadRepo = fileUploadRepo;
        this.fileGroupRepo = fileGroupRepo;
    }

    public FileRelation returnFileRelation(long eventId) {
        Activities activity = activitiesRepo.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        Set<Long> seenEventIds = new HashSet<>();
        Map<Long, FileRelationNodes> nodesMap = new HashMap<>();
        List<FileRelationEdges> edges = new ArrayList<>();
        int maxDepth = 2;

        // BFS 탐색 시작
        exploreFileRelationsBFS(activity, maxDepth, seenEventIds, nodesMap, edges);

        return FileRelation.builder()
                .nodes(new ArrayList<>(nodesMap.values()))
                .edges(edges)
                .build();
    }

    private void exploreFileRelationsBFS(Activities startActivity, int maxDepth,
                                         Set<Long> seenEventIds, Map<Long, FileRelationNodes> nodesMap,
                                         List<FileRelationEdges> edges) {
        Queue<ExplorationNode> queue = new LinkedList<>();
        queue.add(new ExplorationNode(startActivity, 0));

        while (!queue.isEmpty()) {
            ExplorationNode currentNode = queue.poll();
            Activities currentActivity = currentNode.getActivity();
            int currentDepth = currentNode.getDepth();

            if (currentDepth > maxDepth || seenEventIds.contains(currentActivity.getId())) {
                continue;
            }

            seenEventIds.add(currentActivity.getId());
            FileRelationNodes currentFileNode = createFileRelationNodes(currentActivity);
            nodesMap.putIfAbsent(currentActivity.getId(), currentFileNode);

            // 동일한 saasFileId를 가진 파일 찾기
            if (currentActivity.getSaasFileId() != null) {
                List<Activities> sameSaasFiles = activitiesRepo.findListBySaasFileId(currentActivity.getSaasFileId());
                for (Activities a : sameSaasFiles) {
                    if (!seenEventIds.contains(a.getId()) && a.getId() != currentActivity.getId()) {
                        FileRelationNodes targetNode = createFileRelationNodes(a);
                        nodesMap.putIfAbsent(a.getId(), targetNode);
                        edges.add(new FileRelationEdges(currentActivity.getId(), a.getId(), "SAAS_ID_MATCH"));
                        queue.add(new ExplorationNode(a, currentDepth + 1));
                    }
                }
            }

            // 동일한 해시 값을 가진 파일 찾기
            String hash = getSaltedHash(currentActivity.getUser().getOrgSaaS().getId(), currentActivity.getSaasFileId());
            List<Activities> sameHashFiles = activitiesRepo.findByHash(hash);
            for (Activities a : sameHashFiles) {
                if (!seenEventIds.contains(a.getId()) && a.getId() != currentActivity.getId()) {
                    FileRelationNodes targetNode = createFileRelationNodes(a);
                    nodesMap.putIfAbsent(a.getId(), targetNode);
                    edges.add(new FileRelationEdges(currentActivity.getId(), a.getId(), "HASH_MATCH"));
                    queue.add(new ExplorationNode(a, currentDepth + 1));
                }
            }

        }

        // 동일한 그룹의 파일 찾기
        String groupName = fileGroupRepo.findGroupNameById(startActivity.getId());
        long orgId = activitiesRepo.findOrgIdByActivityId(startActivity.getId());
        List<Activities> sameGroups = activitiesRepo.findByOrgIdAndGroupName(orgId, groupName);
        for (Activities a : sameGroups) {
            if(!seenEventIds.contains(a.getId()) && a.getId() != startActivity.getId()){
                FileRelationNodes targetNode = createFileRelationNodes(a);
                nodesMap.putIfAbsent(a.getId(), targetNode);
                edges.add(new FileRelationEdges(startActivity.getId(), a.getId(), "SIMILAR_MATCH"));
            }
        }
    }

    private FileRelationNodes createFileRelationNodes(Activities activity) {
        return FileRelationNodes.builder()
                .eventId(activity.getId())
                .saas(activity.getUser().getOrgSaaS().getSaas().getSaasName())
                .eventType(activity.getEventType())
                .fileName(activity.getFileName())
                .hash256(getSaltedHash(activity.getUser().getOrgSaaS().getId(), activity.getSaasFileId()))
                .saasFileId(activity.getSaasFileId())
                .eventTs(activity.getEventTs())
                .email(activity.getUser().getEmail())
                .uploadChannel(activity.getUploadChannel())
                .build();
    }

    private String getSaltedHash(long orgSaaSId, String saasFileId) {
        return fileUploadRepo.findHashByOrgSaaS_IdAndSaasFileId(orgSaaSId, saasFileId);
    }
}


