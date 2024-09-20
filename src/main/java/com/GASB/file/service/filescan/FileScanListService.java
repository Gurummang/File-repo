package com.GASB.file.service.filescan;

import com.GASB.file.model.dto.response.list.*;
import com.GASB.file.model.entity.*;
import com.GASB.file.repository.file.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@Slf4j
public class FileScanListService {

    private final StoredFileRepo storedFileRepo;
    private final FileUploadRepo fileUploadRepo;
    private final TypeScanRepo typeScanRepo;
    private final ActivitiesRepo activitiesRepo;
    private final GscanRepo gscanRepo;
    private final DlpReportRepo dlpReportRepo;
    private final PolicyRepo policyRepo;
    private final ModelMapper modelMapper;
    private static final String UNKNOWN = "Unknown";

    @Autowired
    public FileScanListService(ModelMapper modelMapper, StoredFileRepo storedFileRepo, PolicyRepo policyRepo, TypeScanRepo typeScanRepo, DlpReportRepo dlpReportRepo, FileUploadRepo fileUploadRepo, ActivitiesRepo activitiesRepo
    ,GscanRepo gscanRepo){
        this.modelMapper = modelMapper;
        this.storedFileRepo = storedFileRepo;
        this.typeScanRepo = typeScanRepo;
        this.fileUploadRepo = fileUploadRepo;
        this.activitiesRepo = activitiesRepo;
        this.gscanRepo = gscanRepo;
        this.dlpReportRepo=dlpReportRepo;
        this.policyRepo=policyRepo;
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

    private List<FileListDto> fetchFileList(long orgId) {
        return fileUploadRepo.findAllByOrgId(orgId)
                .stream()
                .map(fileUpload -> {
                    FileListDto dto = createFileListDto(fileUpload, orgId);
                    if (dto == null) {
                        log.debug("FileListDto is null for fileUpload with id: {}", fileUpload.getId());
                    }
                    return dto;
                })
                .filter(Objects::nonNull)
                .toList();
    }


    private FileListDto createFileListDto(FileUpload fileUpload, long orgId) {
        String hash = fileUpload.getHash();

        Optional<StoredFile> optionalStoredFile = storedFileRepo.findBySaltedHash(hash);
        if (optionalStoredFile.isEmpty()) {
            log.debug("No StoredFile found for hash: {}", hash);
            return null;
        }
        StoredFile storedFile = optionalStoredFile.get();
        VtReport vtReport = storedFile.getVtReport();
        FileStatus fileStatus = storedFile.getFileStatus();
        List<DlpReport> dlpReports = dlpReportRepo.findDlpReportsByUploadIdAndOrgId(storedFile.getId(), orgId);

        Activities activities = getActivities(fileUpload.getSaasFileId(), fileUpload.getTimestamp());
        if (activities == null) {
            log.debug("No Activities found for fileUpload id: {}", fileUpload.getId());
        }
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
                .dlpReport(convertToDlpReportDto(dlpReports))
                .fileStatus(convertToFileStatusDto(fileStatus))
                .gscan(createInnerScanDto(fileUpload.getId(), hash)) // Assuming GScan info should be included
                .build();
    }

    private InnerScanDto createInnerScanDto(long id, String hash) {
        TypeScan typeScan = getTypeScan(id, hash);
        MimeTypeDto mimeTypeDto = (typeScan != null) ? convertToMimeTypeDto(typeScan) : null;

        Gscan gscan = getGscan(hash);
        ScanTableDto scanTableDto = convertToScanTableDto(gscan);
        return InnerScanDto.builder()
                .step1(mimeTypeDto)
                .step2(scanTableDto) // Assuming "null" is a placeholder
                .build();
    }

    private int totalMalwareCount(long orgId) {
        return fileUploadRepo.countVtMalwareByOrgId(orgId) + fileUploadRepo.countSuspiciousMalwareByOrgId(orgId);
    }

    private TypeScan getTypeScan(long id, String hash) {
        return typeScanRepo.findByHash(hash, id).orElse(null);
    }

    private Gscan getGscan(String hash){
        return gscanRepo.findByHash(hash);
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


    private FileStatusDto convertToFileStatusDto(FileStatus fileStatus) {
        if (fileStatus == null) {
            log.debug("FileStatus is null, cannot convert to FileStatusDto");
            return null;
        }
        return modelMapper.map(fileStatus, FileStatusDto.class);
    }

    private DlpReportDto convertToDlpReportDto(List<DlpReport> dlpReports){
        return DlpReportDto.builder()
                .totalDlp(countTotalDlp(dlpReports))
                .totalPolicies(countTotalPolicies(dlpReports))
                .policies(getPolicies(dlpReports)) // 정책 리스트를 필요에 맞게 추가
                .comments(getPoliciesComments(dlpReports))
                .pii(getPiiCounts(dlpReports))
                .build();
    }

    private int countTotalPolicies(List<DlpReport> dlpReports) {
        return (int) dlpReports.stream()
                .filter(report -> report.getInfoCnt() > 0) // infoCnt가 1 이상인 튜플만 필터링
                .map(DlpReport::getPolicy) // 정책을 가져와서
                .distinct() // 중복을 제거하고
                .count(); // 개수를 셉니다
    }


    private int countTotalDlp(List<DlpReport> dlpReports) {
        return dlpReports.stream()
                .mapToInt(DlpReport::getInfoCnt) // 모든 DlpReport의 infoCnt를 가져옵니다
                .sum(); // 전체 합계
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
                .collect(Collectors.toList()); // 최종 리스트로 변환
    }

    private List<String> getPoliciesComments(List<DlpReport> dlpReports) {
        return dlpReports.stream()
                .filter(report -> report.getInfoCnt() > 0) // infoCnt가 1 이상인 튜플만 필터링
                .map(report -> report.getPolicy().getComment()) // 정책의 코멘트 가져오기
                .distinct() // 중복 제거
                .collect(Collectors.toList()); // 리스트로 수집
    }

    private List<PiiDto> getPiiCounts(List<DlpReport> dlpReports) {
        return dlpReports.stream()
                .collect(Collectors.groupingBy(report -> report.getPii().getContent(), // PII별로 그룹화
                        Collectors.summingInt(DlpReport::getInfoCnt))) // 각 그룹의 infoCnt 합산
                .entrySet().stream()
                .map(entry -> new PiiDto(entry.getKey(), entry.getValue())) // PiiDto 생성
                .collect(Collectors.toList()); // 최종 리스트로 수집
    }

}
