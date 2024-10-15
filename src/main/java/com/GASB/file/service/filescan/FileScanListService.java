package com.GASB.file.service.filescan;

import com.GASB.file.model.dto.response.list.*;
import com.GASB.file.model.entity.*;
import com.GASB.file.repository.file.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class FileScanListService {

    private final FileUploadRepo fileUploadRepo;
    private final TypeScanRepo typeScanRepo;
    private final ActivitiesRepo activitiesRepo;
    private final GscanRepo gscanRepo;
    private final DlpReportRepo dlpReportRepo;
    private final ModelMapper modelMapper;
    private static final String UNKNOWN = "Unknown";

    @Autowired
    public FileScanListService(ModelMapper modelMapper, TypeScanRepo typeScanRepo, DlpReportRepo dlpReportRepo, FileUploadRepo fileUploadRepo, ActivitiesRepo activitiesRepo
    ,GscanRepo gscanRepo){
        this.modelMapper = modelMapper;
        this.typeScanRepo = typeScanRepo;
        this.fileUploadRepo = fileUploadRepo;
        this.activitiesRepo = activitiesRepo;
        this.gscanRepo = gscanRepo;
        this.dlpReportRepo=dlpReportRepo;
    }

    public FileListResponse getFileList(long orgId) {
        try {
            List<FileListDto> fileList = fetchFileList(orgId);
            int totalFiles = fileList.size();
            int malwareTotal = totalMalwareCount(orgId);
            int dlpTotal = totalDlpCount(orgId);

            return FileListResponse.of(totalFiles, dlpTotal, malwareTotal, fileList);
        } catch (Exception e) {
            log.error("Error retrieving file list: {}", e.getMessage(), e);
            return FileListResponse.of(0, 0, 0, Collections.emptyList());
        }
    }

    private int totalDlpCount(long orgId) {
        Integer count = fileUploadRepo.countDlpIssuesByOrgId(orgId);
        return count != null ? count : 0;
    }

    @Cacheable("fileUploads")
    public List<FileUpload> findAllByOrgId(long orgId) {
        return fileUploadRepo.findAllByOrgId(orgId);
    }

    @Cacheable("dlpReports")
    public List<DlpReport> findAllDlpReportsByOrgId(long orgId) {
        return dlpReportRepo.findAllDlpReportsByOrgId(orgId);
    }

    private List<FileListDto> fetchFileList(long orgId) {
        // 모든 FileUpload 및 DlpReport를 가져오기
        List<FileUpload> fileUploads = findAllByOrgId(orgId);
        List<DlpReport> allDlpReports = findAllDlpReportsByOrgId(orgId);

        // DlpReport를 StoredFile ID를 기준으로 그룹화
        Map<Long, List<DlpReport>> dlpReportsMap = allDlpReports.stream()
                .collect(Collectors.groupingBy(report -> report.getStoredFile().getId()));

        List<Long> fileUploadIds = fileUploads.stream()
                .map(FileUpload::getId)
                .toList();

        Map<Long, TypeScan> typeScanMap = typeScanRepo.findByFileUploadIds(fileUploadIds)
                .stream()
                .collect(Collectors.toMap(typeScan -> typeScan.getFileUpload().getId(), typeScan -> typeScan));

        List<Long> storedFileIds = fileUploads.stream()
                .map(fileUpload -> fileUpload.getStoredFile().getId())
                .toList();

        Map<Long, Gscan> gscanMap = gscanRepo.findByStoredFileIds(storedFileIds)
                .stream()
                .collect(Collectors.toMap(gscan -> gscan.getStoredFile().getId(), gscan -> gscan));

        return fileUploads.parallelStream()
                .filter(fileUpload -> fileUpload.getStoredFile() != null)  // 미리 필터링// 병렬 처리
                .map(fileUpload -> {
                    // DlpReport와 DlpStat을 매핑하여 FileListDto 생성
                    List<DlpReport> dlpReports = dlpReportsMap.get(fileUpload.getStoredFile().getId());
                    DlpStat dlpStat = fileUpload.getDlpStat();
                    Gscan gscan = gscanMap.get(fileUpload.getStoredFile().getId());
                    StoredFile storedFile = fileUpload.getStoredFile();
                    TypeScan typeScan = typeScanMap.get(fileUpload.getId());
                    return createFileListDto(fileUpload, dlpReports, dlpStat, storedFile, typeScan, gscan);
                })
                .toList();
    }

    private FileListDto createFileListDto(FileUpload fileUpload, List<DlpReport> dlpReports, DlpStat dlpStat, StoredFile storedFile, TypeScan typeScan, Gscan gscan) {
        VtReport vtReport = storedFile.getVtReport();
        FileStatus fileStatus = storedFile.getFileStatus();

        // Activities 데이터를 미리 가져오도록 수정
        Activities activities = getActivities(fileUpload.getSaasFileId(), fileUpload.getTimestamp());

        return FileListDto.builder()
                .id(fileUpload.getId())
                .name(activities != null ? activities.getFileName() : UNKNOWN)
                .size(storedFile.getSize())
                .type(storedFile.getType())
                .saas(activities != null ? activities.getUser().getOrgSaaS().getSaas().getSaasName() : UNKNOWN)
                .user(activities != null ? activities.getUser().getUserName() : UNKNOWN)
                .path(activities != null ? activities.getUploadChannel() : UNKNOWN)
                .date(activities != null ? activities.getEventTs() : null)
                .vtReport(convertToVtReportDto(vtReport))
                .dlpReport(convertToDlpReportDto(dlpReports))  // DlpReport 데이터를 매핑
                .fileStatus(convertToFileStatusDto(fileStatus, dlpStat))  // DlpStat을 함께 처리
                .gscan(createInnerScanDto(typeScan, gscan))
                .build();
    }

    private InnerScanDto createInnerScanDto(TypeScan typeScan, Gscan gscan) {
        MimeTypeDto mimeTypeDto = (typeScan != null) ? convertToMimeTypeDto(typeScan) : null;

        ScanTableDto scanTableDto = convertToScanTableDto(gscan);
        return InnerScanDto.builder()
                .step1(mimeTypeDto)
                .step2(scanTableDto) // Assuming "null" is a placeholder
                .build();
    }

    private int totalMalwareCount(long orgId) {
        return fileUploadRepo.countVtMalwareByOrgId(orgId) + fileUploadRepo.countSuspiciousMalwareByOrgId(orgId);
    }

    private ScanTableDto convertToScanTableDto(Gscan gscan){
        if(gscan == null){
            return null;
        }
        return ScanTableDto.builder()
                .detect(gscan.isDetected())
                .yara(gscan.getStep2Detail() != null ? gscan.getStep2Detail() : "none")
                .build();
    }

    private Activities getActivities(String saasFileId, LocalDateTime timestamp) {
        Activities activities = activitiesRepo.findAllBySaasFileIdAndTimeStamp(saasFileId, timestamp);
        if (activities == null) {
            log.info("No activities found for saasFileId: {} and timestamp: {}", saasFileId, timestamp);
        }
        return activities;
    }


    private MimeTypeDto convertToMimeTypeDto(TypeScan typeScan) {
        if (typeScan == null) {
            log.debug("TypeScan is null, cannot convert to MimeTypeDto");
            return null;
        }
        return modelMapper.map(typeScan, MimeTypeDto.class);
    }

    private VtReportDto convertToVtReportDto(VtReport vtReport) {
        if (vtReport == null) {
            return null;
        }

        return VtReportDto.builder()
                .type(vtReport.getType())
                .sha256(vtReport.getStoredFile().getSaltedHash())  // StoredFile에서 sha256을 가져옴
                .v3(vtReport.getV3())
                .alyac(vtReport.getAlyac())
                .kaspersky(vtReport.getKaspersky())
                .falcon(vtReport.getFalcon())
                .avast(vtReport.getAvast())
                .sentinelone(vtReport.getSentinelone())
                .detectEngine(vtReport.getDetectEngine())
                .completeEngine(vtReport.getCompleteEngine())
                .score(vtReport.getScore())
                .threatLabel(vtReport.getThreatLabel())
                .reportUrl(vtReport.getReportUrl())
                .build();
    }


    private FileStatusDto convertToFileStatusDto(FileStatus fileStatus, DlpStat dlpStat) {
        if (fileStatus == null) {
            log.debug("FileStatus is null, cannot convert to FileStatusDto");
            return null;
        }
        return FileStatusDto.builder()
                .gscanStatus(fileStatus.getGscanStatus())
                .dlpStatus(dlpStat != null ? dlpStat.getDlpStatus() : -1)
                .vtStatus(fileStatus.getVtStatus())
                .build();
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

    private int countTotalPolicies(List<DlpReport> dlpReports) {
        return (int) dlpReports.stream()
                .filter(report -> report.getInfoCnt() > 0) // infoCnt가 1 이상인 튜플만 필터링
                .map(DlpReport::getPolicy)
                .distinct()
                .count();
    }


    private int countTotalDlp(List<DlpReport> dlpReports) {
        return dlpReports.stream()
                .mapToInt(DlpReport::getInfoCnt) // 모든 DlpReport의 infoCnt를 가져옵니다
                .sum();
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
