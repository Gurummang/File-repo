package com.GASB.file.service.filescan;

import com.GASB.file.model.dto.response.list.*;
import com.GASB.file.model.entity.*;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.FileUploadRepo;
import com.GASB.file.repository.file.StoredFileRepo;
import com.GASB.file.repository.file.TypeScanRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@Service
@Slf4j
public class FileScanListService {

    private final StoredFileRepo storedFileRepo;
    private final FileUploadRepo fileUploadRepo;
    private final TypeScanRepo typeScanRepo;
    private final ActivitiesRepo activitiesRepo;
    private final ModelMapper modelMapper;
    private static final String UNKNOWN = "Unknown";

    @Autowired
    public FileScanListService(ModelMapper modelMapper, StoredFileRepo storedFileRepo, TypeScanRepo typeScanRepo, FileUploadRepo fileUploadRepo, ActivitiesRepo activitiesRepo){
        this.modelMapper = modelMapper;
        this.storedFileRepo = storedFileRepo;
        this.typeScanRepo = typeScanRepo;
        this.fileUploadRepo = fileUploadRepo;
        this.activitiesRepo = activitiesRepo;
    }

    public FileListResponse getFileList(long orgId) {
        try {
            List<FileListDto> fileList = fetchFileList(orgId);
            int totalFiles = fileList.size();
            int malwareTotal = totalMalwareCount(orgId);

            return FileListResponse.of(totalFiles, 0, malwareTotal, fileList);
        } catch (Exception e) {
            log.error("Error retrieving file list: {}", e.getMessage(), e);
            return FileListResponse.of(0, 0, 0, Collections.emptyList());
        }
    }

    private List<FileListDto> fetchFileList(long orgId) {
        return fileUploadRepo.findAllByOrgId(orgId)
                .stream()
                .map(fileUpload -> {
                    FileListDto dto = createFileListDto(fileUpload);
                    if (dto == null) {
                        log.debug("FileListDto is null for fileUpload with id: {}", fileUpload.getId());
                    }
                    return dto;
                })
                .filter(Objects::nonNull)
                .toList();
    }


    private FileListDto createFileListDto(FileUpload fileUpload) {
        String hash = fileUpload.getHash();

        Optional<StoredFile> optionalStoredFile = storedFileRepo.findBySaltedHash(hash);
        if (optionalStoredFile.isEmpty()) {
            log.debug("No StoredFile found for hash: {}", hash);
            return null;
        }
        StoredFile storedFile = optionalStoredFile.get();
        VtReport vtReport = storedFile.getVtReport();
        FileStatus fileStatus = storedFile.getFileStatus();

        Activities activities = getActivities(fileUpload.getSaasFileId(), fileUpload.getTimestamp());
        if (activities == null) {
            log.debug("No Activities found for fileUpload id: {}", fileUpload.getId());
        }
        return FileListDto.builder()
                .id(fileUpload.getId())
                .name(activities != null ? activities.getFileName() : UNKNOWN)
                .size(fileUpload.getStoredFile().getSize())
                .type(fileUpload.getStoredFile().getType())
                .saas(activities != null ? activities.getUser().getOrgSaaS().getSaas().getSaasName() : UNKNOWN)
                .user(activities != null ? activities.getUser().getUserName() : UNKNOWN)
                .path(activities != null ? activities.getUploadChannel() : UNKNOWN)
                .date(activities != null ? activities.getEventTs() : null)
                .vtReport(convertToVtReportDto(vtReport))
                .fileStatus(convertToFileStatusDto(fileStatus))
                .gscan(createInnerScanDto(fileUpload.getId(), hash)) // Assuming GScan info should be included
                .build();
    }

    private InnerScanDto createInnerScanDto(long id, String hash) {
        TypeScan typeScan = getTypeScan(id, hash);
        MimeTypeDto mimeTypeDto = (typeScan != null) ? convertToMimeTypeDto(typeScan) : null;

        return InnerScanDto.builder()
                .step1(mimeTypeDto)
                .step2("null") // Assuming "null" is a placeholder
                .build();
    }

    private int totalMalwareCount(long orgId) {
        return fileUploadRepo.countVtMalwareByOrgId(orgId) + fileUploadRepo.countSuspiciousMalwareByOrgId(orgId);
    }

    private TypeScan getTypeScan(long id, String hash) {
        return typeScanRepo.findByHash(hash, id).orElse(null);
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
    }
