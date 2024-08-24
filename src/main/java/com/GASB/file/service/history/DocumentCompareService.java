package com.GASB.file.service.history;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

import com.GASB.file.model.entity.Activities;
import com.GASB.file.model.entity.StoredFile;
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
import org.apache.tika.exception.TikaException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DocumentCompareService {

    private final FileUploadRepo fileUploadRepo;
    private final StoredFileRepo storedFileRepo;
    private final S3FileDownloadService s3FileDownloadService;
    private static final int SHINGLE_LENGTH = 3;

    public DocumentCompareService(FileUploadRepo fileUploadRepo, StoredFileRepo storedFileRepo,
                                  S3FileDownloadService s3FileDownloadService) {
        this.fileUploadRepo = fileUploadRepo;
        this.storedFileRepo = storedFileRepo;
        this.s3FileDownloadService = s3FileDownloadService;
    }

    public double documentSimilar(Activities act, Activities cmpAct) throws IOException, TikaException {
        if (act.getId().equals(cmpAct.getId())) {
            return 1.0;
        }
        String hash1 = getHashForActivity(act);
        String hash2 = getHashForActivity(cmpAct);

        String text1 = parseText(hash1);
        String text2 = parseText(hash2);

        Set<String> shingles1 = createShingles(text1);
        Set<String> shingles2 = createShingles(text2);

        double similarity = calculateJaccardIndex(shingles1, shingles2);
        log.info("Jaccard Similarity: {} ", similarity);
        return similarity;
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
            case "txt", "log", "text" -> extractTextFromTxt(bucketName, key);
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

    private static Set<String> createShingles(String text) {
        Set<String> shingles = new HashSet<>();
        for (int i = 0; i < text.length() - SHINGLE_LENGTH + 1; i++) {
            shingles.add(text.substring(i, i + SHINGLE_LENGTH));
        }
        return shingles;
    }

    public static double calculateJaccardIndex(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        if (union.isEmpty()) {
            return 0.0;
        }

        return (double) intersection.size() / union.size();
    }

    public String extractTextFromTxt(String bucketName, String key) throws IOException {
        StringBuilder text = new StringBuilder();
        try (InputStream fis = s3FileDownloadService.downloadFile(bucketName, key);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(reader)) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                text.append(line).append("\n");
            }
        }
        return text.toString();
    }
}

