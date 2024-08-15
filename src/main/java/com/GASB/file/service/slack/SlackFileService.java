package com.GASB.file.service.slack;
import com.GASB.file.model.dto.response.slack.SlackTotalFileDataDto;
import com.GASB.file.model.entity.*;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.file.StoredFileRepo;
import com.GASB.file.repository.file.VtReportRepo;
import com.GASB.file.repository.file.FileStatusRepo;
import com.GASB.file.repository.org.OrgSaaSRepo;
import com.GASB.file.repository.file.FileUploadRepo;
import com.GASB.file.repository.user.SlackUserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SlackFileService {

    private final OrgSaaSRepo orgSaaSRepo;
    private final SlackUserRepo slackUserRepo;
    private final FileUploadRepo fileUploadRepo;
    private final ActivitiesRepo activitiesRepo;
    private final StoredFileRepo storedFileRepo;
    private final VtReportRepo vtReportRepo;
    private final FileStatusRepo fileStatusRepo;

    @Autowired
    public SlackFileService(SlackUserRepo slackUserRepo, OrgSaaSRepo orgSaaSRepo, ActivitiesRepo activitiesRepo, FileUploadRepo fileUploadRepo,
                            StoredFileRepo storedFileRepo, VtReportRepo vtReportRepo, FileStatusRepo fileStatusRepo){
        this.slackUserRepo = slackUserRepo;
        this.orgSaaSRepo = orgSaaSRepo;
        this.fileUploadRepo = fileUploadRepo;
        this.activitiesRepo = activitiesRepo;
        this.storedFileRepo = storedFileRepo;
        this.vtReportRepo = vtReportRepo;
        this.fileStatusRepo = fileStatusRepo;
    }


    public SlackTotalFileDataDto slackTotalFilesData() {
        List<FileUpload> fileUploads = fileUploadRepo.findAll();
        List<SlackTotalFileDataDto.FileDetail> fileDetails = fileUploads.stream().map(fileUpload -> {
            SlackTotalFileDataDto.FileDetail.FileDetailBuilder detailBuilder = SlackTotalFileDataDto.FileDetail.builder()
                    .fileId(fileUpload.getSaasFileId())
                    .timestamp(fileUpload.getTimestamp());

            Activities activity = activitiesRepo.findBySaasFileId(fileUpload.getSaasFileId()).orElse(null);
            if (activity != null) {
                detailBuilder.fileName(activity.getFileName());
                MonitoredUsers user = slackUserRepo.findByUserId(activity.getUser().getUserId()).orElse(null);
                if (user != null) {
                    detailBuilder.username(user.getUserName());
                    OrgSaaS saas = orgSaaSRepo.findById(user.getOrgSaaS().getId()).orElse(null);
                    if (saas != null) {
                        detailBuilder.saasName(saas.getSaas().getSaasName());
                    } else {
                        detailBuilder.saasName("unknown_saas");
                    }
                } else {
                    detailBuilder.saasName("unknown_saas");
                }
            }

            StoredFile storedFile = storedFileRepo.findBySaltedHash(fileUpload.getHash()).orElse(null);
            if (storedFile != null) {
                detailBuilder.fileType(storedFile.getType())
                        .filePath(Objects.requireNonNull(activity).getUploadChannel());
//                        .filePath(storedFile.getSavePath());

                VtReport vtReport = vtReportRepo.findByStoredFile(storedFile).orElse(null);
                if (vtReport != null) {
                    SlackTotalFileDataDto.FileDetail.VtScanResult vtScanResult = SlackTotalFileDataDto.FileDetail.VtScanResult.builder()
                            .threatLabel(vtReport.getThreatLabel())
                            .hash(fileUpload.getHash())
                            .detectEngine(vtReport.getDetectEngine())
                            .score(vtReport.getScore())
                            .v3(vtReport.getV3())
                            .alyac(vtReport.getAlyac())
                            .kaspersky(vtReport.getKaspersky())
                            .falcon(vtReport.getFalcon())
                            .avast(vtReport.getAvast())
                            .sentinelone(vtReport.getSentinelone())
                            .reportUrl(vtReport.getReportUrl())
                            .build();
                    detailBuilder.vtScanResult(vtScanResult);
                }

                FileStatus fileStatus = fileStatusRepo.findByStoredFile(storedFile);
                if (fileStatus != null) {
                    SlackTotalFileDataDto.FileDetail.GScanResult gScanResult = SlackTotalFileDataDto.FileDetail.GScanResult.builder()
                            .status(String.valueOf(fileStatus.getGscanStatus()))
                            .build();
                    detailBuilder.gScanResult(gScanResult);

                    SlackTotalFileDataDto.FileDetail.DlpScanResult dlpScanResult = SlackTotalFileDataDto.FileDetail.DlpScanResult.builder()
                            .status(String.valueOf(fileStatus.getDlpStatus()))
                            .build();
                    detailBuilder.dlpScanResult(dlpScanResult);
                }
            }

            return detailBuilder.build();
        }).toList();

        SlackTotalFileDataDto totalFileDataDto = new SlackTotalFileDataDto();
        totalFileDataDto.setStatus("success");
        totalFileDataDto.setFiles(fileDetails);

        return totalFileDataDto;
    }
}
