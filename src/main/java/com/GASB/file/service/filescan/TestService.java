package com.GASB.file.service.filescan;

import com.GASB.file.model.dto.response.list.*;
import com.GASB.file.model.entity.*;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.DlpReportRepo;
import com.GASB.file.repository.file.FileUploadRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TestService {

    private final FileUploadRepo fileUploadRepo;
    private final DlpReportRepo dlpReportRepo; // DlpReportRepo 추가
    private final ActivitiesRepo activitiesRepo;
    private static final String UNKNOWN = "Unknown";
    private static final int BATCH_SIZE = 100;  // 배치 크기 설정
    private final ExecutorService executor = Executors.newFixedThreadPool(10); // 스레드 풀 설정

    @Autowired
    public TestService(FileUploadRepo fileUploadRepo, DlpReportRepo dlpReportRepo, ActivitiesRepo activitiesRepo) {
        this.fileUploadRepo = fileUploadRepo;
        this.dlpReportRepo = dlpReportRepo;  // DlpReportRepo 주입
        this.activitiesRepo = activitiesRepo;
    }

    public FileListResponse getFileList(long orgId) {
        try {
            // DlpReport 데이터를 미리 가져오기
            List<DlpReport> allDlpReports = findAllDlpReportsByOrgId(orgId);

            List<FileListDto> fileList = fetchFileList(orgId, allDlpReports);  // DlpReport 리스트 전달
            int totalFiles = fileList.size();
            int malwareTotal = totalMalwareCount(orgId);
            int dlpTotal = totalDlpCount(orgId);

            return FileListResponse.of(totalFiles, dlpTotal, malwareTotal, fileList);
        } catch (Exception e) {
            log.error("Error retrieving file list: {}", e.getMessage(), e);
            return FileListResponse.of(0, 0, 0, List.of());
        }
    }

    private List<FileListDto> fetchFileList(long orgId, List<DlpReport> allDlpReports) throws Exception {
        List<FileScanDto> fileScanList = fileUploadRepo.findFileScanDetailsByOrgId(orgId);

        Map<Long, List<DlpReport>> dlpReportsMap = allDlpReports.stream()
                .collect(Collectors.groupingBy(report -> report.getStoredFile().getId()));

        // 배치 처리
        List<FileListDto> resultList = new ArrayList<>();
        for (int i = 0; i < fileScanList.size(); i += BATCH_SIZE) {
            List<FileScanDto> batch = fileScanList.subList(i, Math.min(i + BATCH_SIZE, fileScanList.size()));
            resultList.addAll(processBatch(batch, dlpReportsMap));  // 배치 단위로 처리
        }

        return resultList;
    }

    // 배치 처리 메서드
    private List<FileListDto> processBatch(List<FileScanDto> batch, Map<Long, List<DlpReport>> dlpReportsMap) throws Exception {
        List<Future<FileListDto>> futures = new ArrayList<>();

        for (FileScanDto fileScanDto : batch) {
            futures.add(executor.submit(() -> createFileListDto(fileScanDto, dlpReportsMap)));
        }

        // Future의 결과를 가져오고 리스트에 추가
        List<FileListDto> resultList = new ArrayList<>();
        for (Future<FileListDto> future : futures) {
            resultList.add(future.get());  // 작업이 완료될 때까지 기다림
        }

        return resultList;
    }

    private FileListDto createFileListDto(FileScanDto fileScanDto, Map<Long, List<DlpReport>> dlpReportsMap) {
        Activities activities = getActivities(fileScanDto.getSaasFileId(), fileScanDto.getTimestamp());

        // DlpReport 데이터를 StoredFile ID로 가져오기
        List<DlpReport> dlpReports = dlpReportsMap.getOrDefault(fileScanDto.getStoredFileId(), Collections.emptyList());

        return FileListDto.builder()
                .id(fileScanDto.getFileUploadId())
                .name(activities != null ? activities.getFileName() : UNKNOWN)
                .size(fileScanDto.getSize())
                .type(fileScanDto.getType())
                .saas(activities != null ? activities.getUser().getOrgSaaS().getSaas().getSaasName() : UNKNOWN)
                .user(activities != null ? activities.getUser().getUserName() : UNKNOWN)
                .path(activities != null ? activities.getUploadChannel() : UNKNOWN)
                .date(activities != null ? activities.getEventTs() : null)
                .vtReport(convertToVtReportDto(fileScanDto))
                .dlpReport(convertToDlpReportDto(dlpReports))  // DlpReport 데이터를 매핑
                .fileStatus(convertToFileStatusDto(fileScanDto))  // DlpStat을 함께 처리
                .gscan(createInnerScanDto(fileScanDto))
                .build();
    }

    @Cacheable("dlpReports")
    public List<DlpReport> findAllDlpReportsByOrgId(long orgId) {
        return dlpReportRepo.findAllDlpReportsByOrgId(orgId);
    }

    private InnerScanDto createInnerScanDto(FileScanDto fileScanDto) {
        MimeTypeDto mimeTypeDto = convertToMimeTypeDto(fileScanDto);

        ScanTableDto scanTableDto = convertToScanTableDto(fileScanDto);
        return InnerScanDto.builder()
                .step1(mimeTypeDto)
                .step2(scanTableDto) // Assuming "null" is a placeholder
                .build();
    }

    private ScanTableDto convertToScanTableDto(FileScanDto fileScanDto){
        if(fileScanDto.getDetect() == null && fileScanDto.getStep2Detail() == null){
            return null;
        }
        return ScanTableDto.builder()
                .detect(fileScanDto.getDetect())
                .yara(fileScanDto.getStep2Detail() != null ? extractYaraList(fileScanDto.getStep2Detail()) : Collections.emptyList())
                .build();
    }

    private List<String> extractYaraList(String step2Detail) {
        String cleaned = step2Detail.replace("dict_keys([", "").replace("])", "");

        return Arrays.stream(cleaned.split(",\\s*"))
                .map(s -> s.replace("'", "")) // 작은 따옴표 제거
                .collect(Collectors.toList()); // 리스트로 변환
    }

    private MimeTypeDto convertToMimeTypeDto(FileScanDto fileScanDto) {
        if (fileScanDto.getCorrect() == null && fileScanDto.getMimeType() == null
                && fileScanDto.getSignature() == null && fileScanDto.getExtension() == null){
            return null;
        }
        return MimeTypeDto.builder()
                .correct(fileScanDto.getCorrect())
                .mimeType(fileScanDto.getMimeType())
                .signature(fileScanDto.getSignature())
                .extension(fileScanDto.getExtension())
                .build();
    }

    private VtReportDto convertToVtReportDto(FileScanDto fileScanDto) {
        if (fileScanDto.getVtType() == null && fileScanDto.getV3() == null
                && fileScanDto.getAlyac() == null && fileScanDto.getKaspersky() == null && fileScanDto.getFalcon() == null
                && fileScanDto.getAvast() == null && fileScanDto.getSentinelone() == null && fileScanDto.getDetectEngine() == null
                && fileScanDto.getCompleteEngine() == null && fileScanDto.getScore() == null && fileScanDto.getThreatLabel() == null
                && fileScanDto.getReportUrl() == null) {
            return null;
        }

        // 각 Integer 필드에서 null 값이 있을 경우 기본값으로 처리
        return VtReportDto.builder()
                .type(fileScanDto.getVtType())
                .sha256(fileScanDto.getHash())
                .v3(fileScanDto.getV3())
                .alyac(fileScanDto.getAlyac())
                .kaspersky(fileScanDto.getKaspersky())
                .falcon(fileScanDto.getFalcon())
                .avast(fileScanDto.getAvast())
                .sentinelone(fileScanDto.getSentinelone())
                .detectEngine(fileScanDto.getDetectEngine() != null ? fileScanDto.getDetectEngine() : 0)  // 기본값 0 설정
                .completeEngine(fileScanDto.getCompleteEngine() != null ? fileScanDto.getCompleteEngine() : 0)  // 기본값 0 설정
                .score(fileScanDto.getScore() != null ? fileScanDto.getScore() : 0)  // 기본값 0 설정
                .threatLabel(fileScanDto.getThreatLabel())
                .reportUrl(fileScanDto.getReportUrl())
                .build();
    }



    private FileStatusDto convertToFileStatusDto(FileScanDto fileScanDto) {
        if (fileScanDto.getGscanStatus() == null && fileScanDto.getDlpStatus() == null && fileScanDto.getVtStatus() == null){
            return FileStatusDto.builder()
                    .gscanStatus(-1)
                    .dlpStatus(-1)
                    .vtStatus(-1)
                    .build();
        }
        return FileStatusDto.builder()
                .gscanStatus(fileScanDto.getGscanStatus())
                .dlpStatus(fileScanDto.getDlpStatus())
                .vtStatus(fileScanDto.getVtStatus())
                .build();
    }

    private Activities getActivities(String saasFileId, LocalDateTime timestamp) {
        Activities activities = activitiesRepo.findAllBySaasFileIdAndTimeStamp(saasFileId, timestamp);
        if (activities == null) {
            log.info("No activities found for saasFileId: {} and timestamp: {}", saasFileId, timestamp);
        }
        return activities;
    }

    private int totalMalwareCount(long orgId) {
        return fileUploadRepo.countVtMalwareByOrgId(orgId) + fileUploadRepo.countSuspiciousMalwareByOrgId(orgId);
    }

    private int totalDlpCount(long orgId) {
        return fileUploadRepo.countDlpIssuesByOrgId(orgId);
    }

    private DlpReportDto convertToDlpReportDto(List<DlpReport> dlpReports) {
        if (dlpReports == null || dlpReports.isEmpty()) {
            return DlpReportDto.builder()
                    .totalDlp(0)
                    .totalPolicies(0)
                    .policies(Collections.emptyList())
                    .comments(Collections.emptyList())
                    .pii(Collections.emptyList())
                    .build();
        }

        return DlpReportDto.builder()
                .totalDlp(countTotalDlp(dlpReports))
                .totalPolicies(countTotalPolicies(dlpReports))
                .policies(getPolicies(dlpReports))
                .comments(getPoliciesComments(dlpReports))
                .pii(getPiiCounts(dlpReports))
                .build();
    }

    private int countTotalDlp(List<DlpReport> dlpReports) {
        return dlpReports.stream()
                .mapToInt(DlpReport::getInfoCnt) // 모든 DlpReport의 infoCnt를 가져옵니다
                .sum();
    }

    private int countTotalPolicies(List<DlpReport> dlpReports) {
        return (int) dlpReports.stream()
                .filter(report -> report.getInfoCnt() > 0) // infoCnt가 1 이상인 튜플만 필터링
                .map(DlpReport::getPolicy)
                .distinct()
                .count();
    }

    private List<PolicyDto> getPolicies(List<DlpReport> dlpReports) {
        return dlpReports.stream()
                .collect(Collectors.groupingBy(report -> report.getPolicy().getId(),
                        Collectors.summingInt(DlpReport::getInfoCnt))) // 정책 ID로 그룹화하고 infoCnt를 합산
                .entrySet().stream()
                .map(entry -> {
                    Policy policy = dlpReports.stream()
                            .filter(report -> report.getPolicy().getId().equals(entry.getKey()))
                            .findFirst() // 첫 번째 DlpReport에서 정책 정보를 가져옴
                            .map(DlpReport::getPolicy)
                            .orElse(null);

                    return new PolicyDto(
                            policy != null ? policy.getPolicyName() : null,
                            entry.getValue() // 탐지된 총 infoCnt
                    );
                })
                .collect(Collectors.toList());
    }

    private List<String> getPoliciesComments(List<DlpReport> dlpReports) {
        return dlpReports.stream()
                .filter(report -> report.getInfoCnt() > 0) // infoCnt가 1 이상인 튜플만 필터링
                .map(report -> report.getPolicy().getComment())
                .distinct()
                .collect(Collectors.toList());
    }

    private List<PiiDto> getPiiCounts(List<DlpReport> dlpReports) {
        return dlpReports.stream()
                .collect(Collectors.groupingBy(report -> report.getPii().getContent(), // PII별로 그룹화
                        Collectors.summingInt(DlpReport::getInfoCnt)))
                .entrySet().stream()
                .map(entry -> new PiiDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
