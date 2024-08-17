package com.GASB.file.service.history;

import org.springframework.stereotype.Service;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
@Service
public class DocumentCompareService {

    public static void documentSimilar() throws IOException{
        String text1 = extractTextFromWord("path/to/first/document.docx");
        String text2 = extractTextFromWord("path/to/second/document.docx");

        Set<String> shingles1 = createShingles(text1, 3); // 3-gram shingles
        Set<String> shingles2 = createShingles(text2, 3);

        double jaccardIndex = calculateJaccardIndex(shingles1, shingles2);
        System.out.println("Jaccard Similarity: " + jaccardIndex);
    }

    // Word 문서에서 텍스트 추출 함수
    private static String extractTextFromWord(String filePath) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        XWPFDocument document = new XWPFDocument(fis);
        StringBuilder text = new StringBuilder();

        List<XWPFParagraph> paragraphs = document.getParagraphs();
        for (XWPFParagraph para : paragraphs) {
            text.append(para.getText());
        }

        document.close();
        fis.close();
        return text.toString();
    }

    // Shingles 생성 함수
    private static Set<String> createShingles(String text, int shingleLength) {
        Set<String> shingles = new HashSet<>();
        for (int i = 0; i < text.length() - shingleLength + 1; i++) {
            shingles.add(text.substring(i, i + shingleLength));
        }
        return shingles;
    }

    // Jaccard 유사도 계산 함수
    private static double calculateJaccardIndex(Set<String> set1, Set<String> set2) {
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }
}
