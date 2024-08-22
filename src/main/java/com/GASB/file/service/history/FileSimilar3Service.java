package com.GASB.file.service.history;

import com.GASB.file.model.dto.response.history.FileRelationNodes;
import com.GASB.file.model.entity.Activities;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileSimilar3Service {

    private final FileSimilarityAsyncService fileSimilarityAsyncService;

    @Autowired
    public FileSimilar3Service(FileSimilarityAsyncService fileSimilarityAsyncService) {
        this.fileSimilarityAsyncService = fileSimilarityAsyncService;
    }

    public List<FileRelationNodes> getFileSimilarity(Activities activity, Set<Activities> nodes) throws IOException, TikaException {
        List<CompletableFuture<FileRelationNodes>> futures = nodes.stream()
                .map(node -> fileSimilarityAsyncService.calculateNodeSimilarity(activity, node))
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
}
