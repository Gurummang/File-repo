package com.GASB.file.controller.slack;
import com.GASB.file.config.slack.ExtractData;
import com.GASB.file.model.dto.response.slack.SlackTotalFileDataDto;
import com.GASB.file.repository.org.AdminRepo;
import com.GASB.file.repository.org.OrgSaaSRepo;
import com.GASB.file.service.slack.SlackFileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files/slack")
public class SlackFileController {
    private final SlackFileService slackFileService;
    private static final Logger logger = LoggerFactory.getLogger(SlackFileController.class);
    private final ExtractData extractData;
    private final OrgSaaSRepo orgSaaSRepo;
    private final AdminRepo adminRepo;

    // 여기서는 space ID가 아니라 클라이언트 인증정보를 알려줘야함 @RequestBody ExtractData request
    // 그래야 orgId에 따른 orgSaaS를 찾아서 그에 맞는 파일을 가져올 수 있음
    @PostMapping("/total")
    public ResponseEntity<SlackTotalFileDataDto> fetchTotalFilesData() {
//        AdminUsers adminUsers = adminRepo.findByEmail(request.getEmail()).orElse(null);
//        OrgSaaS orgSaaSObject = orgSaaSRepo.findByOrgId(adminUsers.getOrg().getId().intValue()).orElse(null);
        try {
            SlackTotalFileDataDto totalFilesData = slackFileService.slackTotalFilesData();
            if (totalFilesData.getFiles().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(SlackTotalFileDataDto.builder()
                        .Status("No Data")
                        .files(Collections.singletonList(SlackTotalFileDataDto.FileDetail.builder()
                                .fileName("No Data")
                                .username("No Data")
                                .fileType("N/A")
                                .timestamp(LocalDateTime.now())
                                .build()))
                        .build());
            }
            return ResponseEntity.ok(totalFilesData);
        } catch (Exception e) {
            logger.error("Error fetching total files data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(SlackTotalFileDataDto.builder()
                    .Status("Error")
                    .files(Collections.singletonList(SlackTotalFileDataDto.FileDetail.builder()
                            .fileName("Error")
                            .username("Server Error")
                            .fileType("N/A")
                            .timestamp(LocalDateTime.now())
                            .build()))
                    .build());
        }
    }
}
