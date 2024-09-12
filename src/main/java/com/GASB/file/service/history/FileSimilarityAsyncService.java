package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.FileRelationNodes;
import com.GASB.file.model.entity.Activities;
import com.GASB.file.model.entity.DlpReport;
import com.GASB.file.model.entity.StoredFile;
import com.GASB.file.model.entity.VtReport;
import com.GASB.file.repository.file.FileUploadRepo;
import com.GASB.file.repository.file.StoredFileRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
                double finalNodeSimilarity = tlshFileComparator.compareFiles(activity,node);
                log.info("node:{}",node.getId());
                return createFileRelationNodes(node, finalNodeSimilarity);
            } catch (Exception e) {
                log.error("Error processing similarity for node: {}", node, e);
                return null; // 에러 발생 시 null 반환
            }
        });
    }

    public FileRelationNodes createFileRelationNodes(Activities activity, double similarity) {
        String hash = getSaltedHash(activity);
        Optional<StoredFile> storedFile = storedFileRepo.findBySaltedHash(hash);
        StoredFile s = storedFile.orElseThrow(() -> new RuntimeException("StoredFile not found"));

        Boolean dlp = Optional.ofNullable(s.getDlpReport())
                .map(DlpReport::getInfoCnt)
                .map(count -> count >= 1)
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
        if ("file_delete".equals(activity.getEventType())) {
            return fileUploadRepo.findLatestHashBySaasFileId(activity.getUser().getOrgSaaS().getId(), activity.getSaasFileId());
        } else {
            return fileUploadRepo.findHashByOrgSaaS_IdAndSaasFileId(activity.getUser().getOrgSaaS().getId(), activity.getSaasFileId(), activity.getEventTs());
        }
    }
}
