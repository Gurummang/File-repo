package com.GASB.file.service.history;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.GASB.file.model.entity.Activities;
import com.GASB.file.model.entity.StoredFile;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileUploadRepo;
import com.GASB.file.repository.file.StoredFileRepo;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Slf4j
public class DocumentCompareService {

    private final ActivitiesRepo activitiesRepo;
    private final FileUploadRepo fileUploadRepo;
    private final StoredFileRepo storedFileRepo;
    private final S3FileDownloadService s3FileDownloadService;
    private static final int SHINGLE_LENGTH = 3;

    public DocumentCompareService(ActivitiesRepo activitiesRepo, FileUploadRepo fileUploadRepo, StoredFileRepo storedFileRepo,
                                  S3FileDownloadService s3FileDownloadService){
        this.activitiesRepo = activitiesRepo;
        this.fileUploadRepo = fileUploadRepo;
        this.storedFileRepo = storedFileRepo;
        this.s3FileDownloadService = s3FileDownloadService;
    }

    public double documentSimilar(Activities act, Activities cmpAct) throws IOException, TikaException {
            if(act.getId().equals(cmpAct.getId())){
                return 1.0;
            }
            String hash1 = getHashForActivity(act);
            String hash2 = getHashForActivity(cmpAct);

            String text1 = parseText(hash1);
            String text2 = parseText(hash2);

            Set<String> shingles1 = createShingles(text1); // 3-gram shingles
            Set<String> shingles2 = createShingles(text2);

            log.info("Jaccard Similarity: {} ", calculateJaccardIndex(shingles1, shingles2));
            return calculateJaccardIndex(shingles1, shingles2);
    }

    private String getHashForActivity(Activities activity){
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

    private String parseText(String hash) throws IOException {
        Optional<StoredFile> storedFileOpt = storedFileRepo.findBySaltedHash(hash);
        if (storedFileOpt.isEmpty()) {
            throw new RuntimeException("File not found in database");
        }

        StoredFile storedFile = storedFileOpt.get();
        String savePath = storedFile.getSavePath();
        String type = storedFile.getType();
        String[] parts = savePath.split("/", 2);

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid savePath format");
        }

        String bucketName = parts[0];
        String key = parts[1];
        return switch (type) {
            case "xlsx", "xls" -> extractTextFromExcel(bucketName, key);
            case "ppt", "pptx" -> extractTextFromPptx(bucketName, key);
            case "doc", "docx" -> extractTextFromDocx(bucketName, key);
            case "pdf" -> extractTextFromPdf(bucketName, key);
            default -> "";
        };
    }

    public String extractTextFromDocx(String bucketName, String key) throws IOException {
        StringBuilder text = new StringBuilder();
        try (InputStream fis = s3FileDownloadService.downloadFile(bucketName, key);
             XWPFDocument document = new XWPFDocument(fis)) {

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
        }
        return text.toString();
    }

    public String extractTextFromPptx(String bucketName, String key) throws IOException {
        StringBuilder text = new StringBuilder();
        try (InputStream fis = s3FileDownloadService.downloadFile(bucketName, key);
             XMLSlideShow ppt = new XMLSlideShow(fis)) {

            for (XSLFSlide slide : ppt.getSlides()) {
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape) {
                        XSLFTextShape textShape = (XSLFTextShape) shape;
                        if (textShape.getText() != null) {
                            text.append(textShape.getText()).append("\n");
                        }
                    }
                }
            }
        }
        return text.toString();
    }

    public String extractTextFromPdf(String bucketName, String key) throws IOException {
        StringBuilder text = new StringBuilder();
        try (InputStream fis = s3FileDownloadService.downloadFile(bucketName, key);
             PDDocument document = PDDocument.load(fis)) {

            // PDF 파일의 모든 페이지에서 텍스트 추출
            PDFTextStripper pdfStripper = new PDFTextStripper();
            text.append(pdfStripper.getText(document));
        }
        return text.toString();
    }

    private String extractTextFromExcel(String bucketName, String key) throws IOException {
        StringBuilder text = new StringBuilder();
        try (InputStream fis = s3FileDownloadService.downloadFile(bucketName, key);
             Workbook workbook = new XSSFWorkbook(fis)) {

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        switch (cell.getCellType()) {
                            case STRING:
                                text.append(cell.getStringCellValue()).append("\n");
                                break;
                            case NUMERIC:
                                text.append(cell.getNumericCellValue()).append("\n");
                                break;
                            case BOOLEAN:
                                text.append(cell.getBooleanCellValue()).append("\n");
                                break;
                            default:
                                // Handle other types of cells if necessary
                                break;
                        }
                    }
                }
            }
        }
        return text.toString();
    }


    // Shingles 생성 함수와 Jaccard 유사도 계산 함수는 동일
    private static Set<String> createShingles(String text) {
        Set<String> shingles = new HashSet<>();
        for (int i = 0; i < text.length() - SHINGLE_LENGTH + 1; i++) {
            shingles.add(text.substring(i, i + SHINGLE_LENGTH));
        }
        // 로그 추가
        log.info("Created shingles: {}", shingles);
        return shingles;
    }


    public static double calculateJaccardIndex(Set<String> set1, Set<String> set2) {
        // 교집합 계산
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        // 합집합 계산
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        // 유사도 계산: 합집합이 비어 있으면 0.0 반환
        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }
}

