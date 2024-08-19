package com.GASB.file.service.history;

import com.GASB.file.model.entity.*;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileGroupRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.text.similarity.JaroWinklerSimilarity;
import org.apache.tika.exception.TikaException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Service
@Slf4j
public class FileSimilarService {

    private final ActivitiesRepo activitiesRepo;
    private final FileGroupRepo fileGroupRepo;
    private final DocumentCompareService documentCompareService;

    @Autowired
    public FileSimilarService(ActivitiesRepo activitiesRepo, FileGroupRepo fileGroupRepo, DocumentCompareService documentCompareService) {
        this.activitiesRepo = activitiesRepo;
        this.fileGroupRepo = fileGroupRepo;
        this.documentCompareService = documentCompareService;
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

    public double getFileSimilarity(Long actId, Long cmpId) {

        // 1. actId로 activities 객체 조회
        Optional<Activities> activity = activitiesRepo.findById(actId);
        Optional<Activities> cmpAct = activitiesRepo.findById(cmpId);
        if (activity.isEmpty() || cmpAct.isEmpty()) {
            return 404; // 해당 객체가 없음
        }

        // 2. 확장자 추출 및 유사도 계산
        String actExtension = FilenameUtils.getExtension(activity.get().getFileName()).toLowerCase();
        String cmpExtension = FilenameUtils.getExtension(cmpAct.get().getFileName()).toLowerCase();
        double typeSimilarity = typeSim(actExtension, cmpExtension);

        // 3. 파일 이름 유사도 계산
        String actFileName = noExtension(activity.get().getFileName());
        String cmpFileName = noExtension(cmpAct.get().getFileName());
        double nameSimilarity = calculateSim(actFileName, cmpFileName);

        String actType = fileGroupRepo.findGroupTypeById(actId);
        String cmpType = fileGroupRepo.findGroupTypeById(cmpId);
        // 문서 유사도 계산
        double fileSimilar;
        try {
            if(actType.equals("document") && cmpType.equals("document")) {
                fileSimilar = documentCompareService.documentSimilar(activity.get(), cmpAct.get());
                log.info("{} {} {}",nameSimilarity, typeSimilarity, fileSimilar);
                return (nameSimilarity * 0.6 + typeSimilarity * 0.4) * 0.4 + fileSimilar * 0.6;
            }
        } catch (IOException | TikaException e) {
            // 예외 처리
            log.info(e.getMessage());
            return 0; // 유사도 계산 실패
        }

        // 4. 총 유사도 계산 (이름 유사도 60% + 확장자 유사도 40%)
        return (nameSimilarity * 0.6) + (typeSimilarity * 0.4);
    }
}
