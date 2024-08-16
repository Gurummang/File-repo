package com.GASB.file.model.dto.response.slack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlackTotalFileDataDto {

    private static final String UNKNOWN = "unknown";
    private static final String TROJAN = "Trojan";
    private String status;
    private List<FileDetail> files;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FileDetail {
        private String fileId;
        private LocalDateTime timestamp;
        private String fileName;
        private String fileType;
        @Builder.Default
        private String saasName = "slack";
        private String username;
        private String filePath;
        private GScanResult gScanResult;
        private VtScanResult vtScanResult;
        private DlpScanResult dlpScanResult;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class GScanResult {
            @Builder.Default
            private String status = UNKNOWN;
            @Builder.Default
            private String typeMatch = "false";
            @Builder.Default
            private List<String> detail = Collections.emptyList();
        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class VtScanResult {
            @Builder.Default
            private String threatLabel = TROJAN;

            @Builder.Default
            private String hash = "03c7c0ace395d80182db07ae2c30f034";

            @Builder.Default
            private int detectEngine = 4;
            @Builder.Default
            private int score = 60;

            @Builder.Default
            private String v3 = TROJAN;
            @Builder.Default
            private String alyac = TROJAN;
            @Builder.Default
            private String kaspersky = TROJAN;
            @Builder.Default
            private String falcon = TROJAN;
            @Builder.Default
            private String avast = TROJAN;
            @Builder.Default
            private String sentinelone = TROJAN;
            @Builder.Default
            private String reportUrl = "https://www.virustotal.com/gui/file/03c7c0ace395d80182db07ae2c30f034/detection";
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class DlpScanResult {
            @Builder.Default
            private String status = UNKNOWN;
            @Builder.Default
            private String result = UNKNOWN;
            @Builder.Default
            private String details = UNKNOWN;
            @Builder.Default
            private List<String> sensitiveDataTypes = Collections.emptyList();
            @Builder.Default
            private int instancesFound = 0;
        }
    }
}

