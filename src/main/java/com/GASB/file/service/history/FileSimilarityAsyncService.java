package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.FileRelationNodes;
import com.GASB.file.model.entity.Activities;
import com.GASB.file.model.entity.DlpReport;
import com.GASB.file.model.entity.StoredFile;
import com.GASB.file.model.entity.VtReport;
import com.GASB.file.repository.file.FileGroupRepo;
import com.GASB.file.repository.file.FileUploadRepo;
import com.GASB.file.repository.file.StoredFileRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.object.StoredProcedure;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class FileSimilarityAsyncService {

    private final FileUploadRepo fileUploadRepo;
    private final StoredFileRepo storedFileRepo;
    private final TlshFileComparator tlshFileComparator;

    @Autowired
    public FileSimilarityAsyncService(FileUploadRepo fileUploadRepo, StoredFileRepo storedFileRepo, TlshFileComparator tlshFileComparator) {
        this.fileUploadRepo = fileUploadRepo;
        this.storedFileRepo = storedFileRepo;
        this.tlshFileComparator = tlshFileComparator;
    }

    @Async
    public CompletableFuture<FileRelationNodes> calculateNodeSimilarity(Activities activity, Activities node) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 확장자 유사도 계산
//                String actExtension = FilenameUtils.getExtension(activity.getFileName()).toLowerCase();
//                String nodeExtension = FilenameUtils.getExtension(node.getFileName()).toLowerCase();
//                double typeSimilarity = typeSim(actExtension, nodeExtension);
//
//                // 파일명 유사도 계산
//                String actFileName = noExtension(activity.getFileName());
//                String nodeFileName = noExtension(node.getFileName());
//                double nameSimilarity = calculateSim(actFileName, nodeFileName);
//                    double fileSimilarity = documentCompareService.documentSimilar(activity, node);
//                    log.info("{} {} {}",nameSimilarity, typeSimilarity, fileSimilarity);
//                    finalNodeSimilarity = (nameSimilarity * 0.6 + typeSimilarity * 0.4) * 0.4 + fileSimilarity * 0.6;


                double finalNodeSimilarity = tlshFileComparator.compareFiles(activity,node);
                // 노드 생성 및 반환
                return createFileRelationNodes(node, finalNodeSimilarity);
            } catch (Exception e) {
                log.error("Error processing similarity for node: {}", node, e);
                return null; // 에러 발생 시 null 반환
            }
        });
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

    private String noExtension(String fileName) {
        return FilenameUtils.getBaseName(fileName).toLowerCase();  // Convert to lower case for consistency
    }

    private double calculateSim(String a, String b) {
        JaroWinklerSimilarity similarity = new JaroWinklerSimilarity();
        return similarity.apply(a, b);
    }

    public FileRelationNodes createFileRelationNodes(Activities activity, double similarity) {
        String hash = getSaltedHash(activity);
        Optional<StoredFile> storedFile = storedFileRepo.findBySaltedHash(hash);
        StoredFile s = storedFile.orElseThrow(() -> new RuntimeException("StoredFile not found"));

        Boolean dlp = Optional.ofNullable(s.getDlpReport())
                .map(DlpReport::getDlp)
                .orElse(false);

        return FileRelationNodes.builder()
                .eventId(activity.getId())
                .saas(activity.getUser().getOrgSaaS().getSaas().getSaasName())
                .eventType(activity.getEventType())
                .fileName(activity.getFileName())
                .hash256(hash)
                .saasFileId(activity.getSaasFileId())
                .eventTs(activity.getEventTs())
                .email(activity.getUser().getEmail())
                .uploadChannel(activity.getUploadChannel())
                .similarity(similarity)
                .dlp(dlp)
                .threat(hasThreatLabel(s.getVtReport())) // 위에서 설정한 boolean 값 사용
                .build();
    }


    public boolean hasThreatLabel(VtReport vtReport) {
        if (vtReport == null) {
            return false;
        }

        return !("none".equals(vtReport.getThreatLabel()));
    }

    private String getSaltedHash(Activities activity) {
        return fileUploadRepo.findHashByOrgSaaS_IdAndSaasFileId(activity.getUser().getOrgSaaS().getId(), activity.getSaasFileId(), activity.getEventTs());
    }
}
