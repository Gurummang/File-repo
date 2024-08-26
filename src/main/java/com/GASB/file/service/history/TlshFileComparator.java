package com.GASB.file.service.history;

import com.GASB.file.model.entity.Activities;
import com.GASB.file.model.entity.StoredFile;
import com.GASB.file.repository.file.FileUploadRepo;
import com.GASB.file.repository.file.StoredFileRepo;
import com.GASB.file.service.tlsh.Tlsh;
import com.GASB.file.service.tlsh.TlshCreator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class TlshFileComparator {

    private final S3FileDownloadService s3FileDownloadService;
    private final FileUploadRepo fileUploadRepo;
    private final StoredFileRepo storedFileRepo;

    public TlshFileComparator(S3FileDownloadService s3FileDownloadService, FileUploadRepo fileUploadRepo, StoredFileRepo storedFileRepo) {
        this.s3FileDownloadService = s3FileDownloadService;
        this.fileUploadRepo = fileUploadRepo;
        this.storedFileRepo = storedFileRepo;
    }

    public double compareFiles(Activities act, Activities cmpAct) {
        try {
            if (act.getId().equals(cmpAct.getId())) {
                return 100;
            }
            // 첫 번째 파일 해시 계산
            Tlsh hash1 = computeTlsHash(act);
            log.info("Hash 1: " + hash1.getEncoded());

            // 두 번째 파일 해시 계산
            Tlsh hash2 = computeTlsHash(cmpAct);
            log.info("Hash 2: " + hash2.getEncoded());

            // 두 해시 간의 차이 계산
            int diff = hash1.totalDiff(hash2, true);

            int maxDiff = 1000; // 예를 들어 최대 256점이라고 가정
            log.info("diff: {}", diff);
            // 유사도 퍼센트 계산
            double similarityPercentage = (1 - (double) diff / maxDiff) * 100;

            // 결과 출력
            log.info("Similarity between files: " + similarityPercentage + "%");
            return similarityPercentage;

        } catch (IOException e) {
            log.error("Error comparing files: {}", e.getMessage(), e);
            return -999;
        }
    }

    private String getHashForActivity(Activities activity) {
        long orgSaasId = activity.getUser().getOrgSaaS().getId();
        String eventType = activity.getEventType();
        String saasFileId = activity.getSaasFileId();
        LocalDateTime eventTs = activity.getEventTs();

        if ("file_upload".equals(eventType) || "file_change".equals(eventType)) {
            return fileUploadRepo.findHashByOrgSaaS_IdAndSaasFileId(orgSaasId, saasFileId, eventTs);
        } else if ("file_delete".equals(eventType)) {
            return fileUploadRepo.findLatestHashBySaasFileId(orgSaasId, saasFileId);
        } else {
            throw new IllegalArgumentException("Unsupported event type: " + eventType);
        }
    }

    private Tlsh computeTlsHash(Activities activities) throws IOException {
        TlshCreator tlshCreator = new TlshCreator();
        final int BUFFER_SIZE = 4096;

        String hash = getHashForActivity(activities);
        Optional<StoredFile> storedFileOpt = storedFileRepo.findBySaltedHash(hash);
        if (storedFileOpt.isEmpty()) {
            throw new RuntimeException("File not found in database");
        }

        StoredFile storedFile = storedFileOpt.get();
        String savePath = storedFile.getSavePath();
        String[] parts = savePath.split("/", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid savePath format");
        }

        String bucketName = parts[0];
        String key = parts[1];

        try (InputStream is = s3FileDownloadService.downloadFile(bucketName, key)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead = is.read(buf, 0, buf.length);
            while (bytesRead >= 0) {
                tlshCreator.update(buf, 0, bytesRead);
                bytesRead = is.read(buf, 0, buf.length);
            }
        }

        return tlshCreator.getHash();
    }
}


