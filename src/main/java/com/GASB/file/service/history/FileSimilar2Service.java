package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.FileRelationNodes;
import com.GASB.file.model.entity.Activities;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileGroupRepo;
import com.GASB.file.repository.file.FileUploadRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileSimilar2Service {

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ActivitiesRepo activitiesRepo;
    private final FileGroupRepo fileGroupRepo;
    private final FileUploadRepo fileUploadRepo;
    private final DocumentCompareService documentCompareService;

    @Autowired
    public FileSimilar2Service(ActivitiesRepo activitiesRepo, FileUploadRepo fileUploadRepo ,FileGroupRepo fileGroupRepo, DocumentCompareService documentCompareService) {
        this.activitiesRepo = activitiesRepo;
        this.fileGroupRepo = fileGroupRepo;
        this.documentCompareService = documentCompareService;
        this.fileUploadRepo = fileUploadRepo;
    }

    // 유사도 측정
    private double calculateSim(String a, String b) {
        JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();
        return similarity.apply(a, b);
    }

    // 파일네임에서 확장자 제거
    private String noExtension(String fileName) {
        return FilenameUtils.getBaseName(fileName).toLowerCase();  // Convert to lower case for consistency
    }

    private String determineExtension(String extension) {
        return switch (extension) {
            // document
            case "doc", "docx", "hwp" -> "group_doc";
            case "ppt", "pptx" -> "group_ppt";
            case "xls", "xlsx", "csv" -> "group_excel";
            case "pdf" -> "group_pdf";
            case "txt" -> "group_txt";
            // image
            case "jpg", "jpeg", "png", "webp" -> "group_snap";
            case "gif" -> "group_gif";
            case "svg" -> "group_svg";
            // exe
            case "exe" -> "group_exe";
            case "dll" -> "group_dll";
            case "elf" -> "group_elf";
            // default
            default -> "Unknown";
        };
    }

    // 파일 확장자의 연관성 계산 메서드
    private double typeSim(String ext1, String ext2) {
        String group1 = determineExtension(ext1);
        String group2 = determineExtension(ext2);

        if (group1.equals(group2)) {
            return 1.0;  // 같은 그룹 내에서는 유사도 1.0
        }

        // PDF는 0.7
        if ((group1.equals("group_pdf") && (group2.equals("group_doc") || group2.equals("group_ppt") || group2.equals("group_excel")))
                || (group2.equals("group_pdf") && (group1.equals("group_doc") || group1.equals("group_ppt") || group1.equals("group_excel")))) {
            return 0.7;
        }

        // 다른 그룹 간의 유사도 0.4
        return 0.4;
    }

//    public double getFileSimilarity(Long eventId, Set<Activities> nodes) {
//
//        // 1. actId로 activities 객체 조회
//        Optional<Activities> activity = activitiesRepo.findById(eventId);
//        Optional<Activities> cmpAct = activitiesRepo.findById(cmpId);
//        if (activity.isEmpty() || cmpAct.isEmpty()) {
//            return 404; // 해당 객체가 없음
//        }
//
//        // 2. 확장자 추출 및 유사도 계산
//        String actExtension = FilenameUtils.getExtension(activity.get().getFileName()).toLowerCase();
//        String cmpExtension = FilenameUtils.getExtension(cmpAct.get().getFileName()).toLowerCase();
//        double typeSimilarity = typeSim(actExtension, cmpExtension);
//
//        // 3. 파일 이름 유사도 계산
//        String actFileName = noExtension(activity.get().getFileName());
//        String cmpFileName = noExtension(cmpAct.get().getFileName());
//        double nameSimilarity = calculateSim(actFileName, cmpFileName);
//
//        String actType = fileGroupRepo.findGroupTypeById(actId);
//        String cmpType = fileGroupRepo.findGroupTypeById(cmpId);
//        // 문서 유사도 계산
//        double fileSimilar;
//        try {
//            if(actType.equals("document") && cmpType.equals("document")) {
//                fileSimilar = documentCompareService.documentSimilarInParallel(eventId, nodes);
//                log.info("{} {} {}",nameSimilarity, typeSimilarity, fileSimilar);
//                return (nameSimilarity * 0.6 + typeSimilarity * 0.4) * 0.4 + fileSimilar * 0.6;
//            }
//        } catch (IOException | TikaException e) {
//            // 예외 처리
//            log.info(e.getMessage());
//            return 0; // 유사도 계산 실패
//        }
//
//        // 4. 총 유사도 계산 (이름 유사도 60% + 확장자 유사도 40%)
//        return (nameSimilarity * 0.6) + (typeSimilarity * 0.4);
//    }

    public List<FileRelationNodes> getFileSimilarity(Activities activity, Set<Activities> nodes) throws IOException, TikaException {
        List<CompletableFuture<FileRelationNodes>> futures = nodes.stream()
                .map(node -> calculateNodeSimilarity(activity, node))
                .collect(Collectors.toList());

        // 모든 비동기 작업이 완료될 때까지 기다리고 결과를 수집
        CompletableFuture<List<FileRelationNodes>> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join) // 모든 결과를 조인하여 리스트로 변환
                        .filter(result -> result != null) // null 제거
                        .collect(Collectors.toList()));

        List<FileRelationNodes> fileRelationNodesList;
        try {
            fileRelationNodesList = allOf.join();
        } catch (Exception e) {
            log.error("Error collecting file relation nodes", e);
            fileRelationNodesList = new ArrayList<>(); // 에러 발생 시 빈 리스트 반환
        }

        return fileRelationNodesList;
    }

    @Async
    public CompletableFuture<FileRelationNodes> calculateNodeSimilarity(Activities activity, Activities node) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 확장자 유사도 계산
                String actExtension = FilenameUtils.getExtension(activity.getFileName()).toLowerCase();
                String nodeExtension = FilenameUtils.getExtension(node.getFileName()).toLowerCase();
                double typeSimilarity = typeSim(actExtension, nodeExtension);

                // 파일명 유사도 계산
                String actFileName = noExtension(activity.getFileName());
                String nodeFileName = noExtension(node.getFileName());
                double nameSimilarity = calculateSim(actFileName, nodeFileName);

                // 문서 유사도 계산
                double fileSimilarity = documentCompareService.documentSimilar(activity, node);

                // 최종 유사도 계산
                double finalNodeSimilarity = (nameSimilarity * 0.6 + typeSimilarity * 0.4) * 0.4 + fileSimilarity * 0.6;

                // 노드 생성 및 반환
                return createFileRelationNodes(node, finalNodeSimilarity);
            } catch (IOException | TikaException e) {
                log.error("Error processing similarity for node: {}", node, e);
                return null; // 에러 발생 시 null 반환
            }
        }, executorService);
    }

    public FileRelationNodes createFileRelationNodes(Activities activity, double similarity) {
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
                .similarity(similarity)
                .build();
    }

    private String getSaltedHash(Activities activity) {
        return fileUploadRepo.findHashByOrgSaaS_IdAndSaasFileId(activity.getUser().getOrgSaaS().getId(), activity.getSaasFileId(), activity.getEventTs());
    }

}

