package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.*;
import com.GASB.file.model.entity.Activities;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileGroupRepo;
import com.GASB.file.repository.file.FileUploadRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class FileVisualizeService {

    private final ActivitiesRepo activitiesRepo;
    private final FileUploadRepo fileUploadRepo;
    private final FileGroupRepo fileGroupRepo;
    private final FileSimilarService fileSimilarService;

    public FileVisualizeService(ActivitiesRepo activitiesRepo, FileUploadRepo fileUploadRepo, FileGroupRepo fileGroupRepo, FileSimilarService fileSimilarService){
        this.activitiesRepo = activitiesRepo;
        this.fileUploadRepo = fileUploadRepo;
        this.fileGroupRepo = fileGroupRepo;
        this.fileSimilarService = fileSimilarService;
    }

    public FileHistoryBySaaS getFileHistoryBySaaS(long eventId) {
        Activities activity = getActivity(eventId);
        Map<String, List<FileRelationNodes>> fileHistoryMap = initializeFileHistoryMap();

        Set<Long> seenEventIds = new HashSet<>();
        Map<Long, FileRelationNodes> nodesMap = new HashMap<>();
        List<FileRelationEdges> edges = new ArrayList<>();
        int maxDepth = 2;

        exploreFileRelationsBFS(activity, maxDepth, seenEventIds, nodesMap, edges);

        String saasName = getSaasName(activity);
        List<FileRelationNodes> nodesList = new ArrayList<>(nodesMap.values());
        populateFileHistoryMap(fileHistoryMap, saasName, nodesList);

        return FileHistoryBySaaS.builder()
                .slack(fileHistoryMap.get("slack"))
                .googleDrive(fileHistoryMap.get("googleDrive"))
                .edges(edges)
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

    private void exploreFileRelationsBFS(Activities startActivity, int maxDepth, Set<Long> seenEventIds, Map<Long, FileRelationNodes> nodesMap, List<FileRelationEdges> edges) {
        Queue<ExplorationNode> queue = new LinkedList<>();
        queue.add(new ExplorationNode(startActivity, 0));

        while (!queue.isEmpty()) {
            ExplorationNode currentNode = queue.poll();
            Activities currentActivity = currentNode.getActivity();
            int currentDepth = currentNode.getDepth();

            if (currentDepth > maxDepth || seenEventIds.contains(currentActivity.getId())) {
                continue;
            }

            processCurrentActivity(currentActivity, startActivity.getId(), seenEventIds, nodesMap, edges);
            queue.addAll(findRelatedActivities(currentActivity, startActivity.getId(), seenEventIds, queue, edges));
        }

        addGroupRelatedActivities(startActivity, seenEventIds, nodesMap);
    }

    private void processCurrentActivity(Activities currentActivity, long startActivityId, Set<Long> seenEventIds, Map<Long, FileRelationNodes> nodesMap, List<FileRelationEdges> edges) {
        seenEventIds.add(currentActivity.getId());
        FileRelationNodes currentFileNode = createFileRelationNodes(currentActivity, startActivityId);
        nodesMap.putIfAbsent(currentActivity.getId(), currentFileNode);
    }

    private List<ExplorationNode> findRelatedActivities(Activities currentActivity, long startActivityId, Set<Long> seenEventIds, Queue<ExplorationNode> queue, List<FileRelationEdges> edges) {
        List<ExplorationNode> additionalNodes = new ArrayList<>();

        addSaasFileIdMatches(currentActivity, startActivityId, seenEventIds, edges, additionalNodes);
        addHashMatches(currentActivity, startActivityId, seenEventIds, additionalNodes);

        return additionalNodes;
    }

    private void addSaasFileIdMatches(Activities currentActivity, long startActivityId, Set<Long> seenEventIds, List<FileRelationEdges> edges, List<ExplorationNode> additionalNodes) {
        if (currentActivity.getSaasFileId() != null) {
            List<Activities> sameSaasFiles = activitiesRepo.findListBySaasFileId(currentActivity.getSaasFileId());
            for (Activities a : sameSaasFiles) {
                if (!seenEventIds.contains(a.getId()) && !a.getId().equals(currentActivity.getId())) {
                    FileRelationNodes targetNode = createFileRelationNodes(a, startActivityId);
                    additionalNodes.add(new ExplorationNode(a, 1));
                    edges.add(new FileRelationEdges(currentActivity.getId(), a.getId(), "SaaS_FileId_Match"));
                }
            }
        }
    }

    private void addHashMatches(Activities currentActivity, long startActivityId, Set<Long> seenEventIds, List<ExplorationNode> additionalNodes) {
        String hash = getSaltedHash(currentActivity.getUser().getOrgSaaS().getId(), currentActivity.getSaasFileId());
        List<Activities> sameHashFiles = activitiesRepo.findByHash(hash);
        for (Activities a : sameHashFiles) {
            if (!seenEventIds.contains(a.getId()) && !a.getId().equals(currentActivity.getId())) {
                FileRelationNodes targetNode = createFileRelationNodes(a, startActivityId);
                additionalNodes.add(new ExplorationNode(a, 1));
            }
        }
    }

    private void addGroupRelatedActivities(Activities startActivity, Set<Long> seenEventIds, Map<Long, FileRelationNodes> nodesMap) {
        String groupName = fileGroupRepo.findGroupNameById(startActivity.getId());
        long orgId = activitiesRepo.findOrgIdByActivityId(startActivity.getId());
        List<Activities> sameGroups = activitiesRepo.findByOrgIdAndGroupName(orgId, groupName);
        for (Activities a : sameGroups) {
            if (!seenEventIds.contains(a.getId()) && !a.getId().equals(startActivity.getId())) {
                FileRelationNodes targetNode = createFileRelationNodes(a, startActivity.getId());
                nodesMap.putIfAbsent(a.getId(), targetNode);
            }
        }
    }

    private FileRelationNodes createFileRelationNodes(Activities activity, long actId) {
        double similar = fileSimilarService.getFileSimilarity(actId, activity.getId());
        BigDecimal roundedSimilarity = new BigDecimal(similar).setScale(2, RoundingMode.HALF_UP);
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
                .similarity(roundedSimilarity.doubleValue())
                .build();
    }

    private String getSaltedHash(long orgSaaSId, String saasFileId) {
        return fileUploadRepo.findHashByOrgSaaS_IdAndSaasFileId(orgSaaSId, saasFileId);
    }
}


