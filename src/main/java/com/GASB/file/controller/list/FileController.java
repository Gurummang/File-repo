package com.GASB.file.controller.list;

import com.GASB.file.annotation.JWT.ValidateJWT;
import com.GASB.file.model.dto.request.EventIdRequest;
import com.GASB.file.model.dto.response.dashboard.FileDashboardDto;
import com.GASB.file.model.dto.response.history.*;
import com.GASB.file.model.dto.response.list.FileListResponse;
import com.GASB.file.model.dto.response.list.ResponseDto;
import com.GASB.file.model.entity.AdminUsers;
import com.GASB.file.repository.file.ActivitiesRepo;
import com.GASB.file.repository.org.AdminRepo;
import com.GASB.file.service.dashboard.FileBoardReturnService;
import com.GASB.file.service.filescan.FileScanListService;
import com.GASB.file.service.filescan.TestService;
import com.GASB.file.service.history.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileBoardReturnService fileBoardReturnService;
    private final FileHistoryService fileHistoryService;
    private final FileHistoryStatisticsService fileHistoryStatisticsService;
    private final VisualizeService fileVisualizeTestService;
    private final FileScanListService fileScanListService;
    private final TestService testService;
    private final AdminRepo adminRepo;
    private final ActivitiesRepo activitiesRepo;
    private static final String INVALID_JWT_MSG = "Invalid JWT: email attribute is missing.";
    private static final String ERROR = "error";
    private static final String EMAIL = "email";
    private static final String EMAIL_NOT_FOUND = "Admin not found with email: ";

    @Autowired
    public FileController(TestService testService, FileBoardReturnService fileBoardReturnService, FileHistoryService fileHistoryService, FileHistoryStatisticsService fileHistoryStatisticsService,
                          VisualizeService fileVisualizeTestService, FileScanListService fileScanListService, AdminRepo adminRepo, ActivitiesRepo activitiesRepo){
        this.fileBoardReturnService = fileBoardReturnService;
        this.fileHistoryService = fileHistoryService;
        this.fileHistoryStatisticsService = fileHistoryStatisticsService;
        this.fileVisualizeTestService = fileVisualizeTestService;
        this.fileScanListService = fileScanListService;
        this.adminRepo = adminRepo;
        this.activitiesRepo = activitiesRepo;
        this.testService = testService;
    }

    @GetMapping("/board")
    @ValidateJWT
    public ResponseDto<FileDashboardDto> fileDashboardList(HttpServletRequest servletRequest){
        if (servletRequest.getAttribute(ERROR) != null) {
            String errorMessage = (String) servletRequest.getAttribute(ERROR);
            return ResponseDto.ofFail(errorMessage);
        }
        String email = (String) servletRequest.getAttribute(EMAIL);

        if (email == null) {
            return ResponseDto.ofFail(INVALID_JWT_MSG);
        }

        Optional<AdminUsers> adminOptional = adminRepo.findByEmail(email);
        if (adminOptional.isEmpty()) {
            return ResponseDto.ofFail(EMAIL_NOT_FOUND + email);
        }

        long orgId = adminOptional.get().getOrg().getId();
        try {
            FileDashboardDto fileDashboard = fileBoardReturnService.boardListReturn(orgId);
            return ResponseDto.ofSuccess(fileDashboard);
        } catch (RuntimeException e) {
            return ResponseDto.ofFail(e.getMessage());
        }
    }

    @GetMapping("/history")
    @ValidateJWT
    public ResponseDto<List<FileHistoryListDto>> fileHistoryList(HttpServletRequest servletRequest) {
        if (servletRequest.getAttribute(ERROR) != null) {
            String errorMessage = (String) servletRequest.getAttribute(ERROR);
            return ResponseDto.ofFail(errorMessage);
        }
        String email = (String) servletRequest.getAttribute(EMAIL);

        if (email == null) {
            return ResponseDto.ofFail(INVALID_JWT_MSG);
        }

        Optional<AdminUsers> adminOptional = adminRepo.findByEmail(email);
        if (adminOptional.isEmpty()) {
            return ResponseDto.ofFail(EMAIL_NOT_FOUND + email);
        }

        long orgId = adminOptional.get().getOrg().getId();
        try {
            List<FileHistoryListDto> fileHistory = fileHistoryService.historyListReturn(orgId);
            return ResponseDto.ofSuccess(fileHistory);
        } catch (RuntimeException e) {
            return ResponseDto.ofFail(e.getMessage());
        }
    }

    @GetMapping("/history/statistics")
    @ValidateJWT
    public ResponseDto<FileHistoryTotalDto> fileHistoryStatisticsList(HttpServletRequest servletRequest){
        if (servletRequest.getAttribute(ERROR) != null) {
            String errorMessage = (String) servletRequest.getAttribute(ERROR);
            return ResponseDto.ofFail(errorMessage);
        }
        String email = (String) servletRequest.getAttribute(EMAIL);

        if (email == null) {
            return ResponseDto.ofFail(INVALID_JWT_MSG);
        }

        Optional<AdminUsers> adminOptional = adminRepo.findByEmail(email);
        if (adminOptional.isEmpty()) {
            return ResponseDto.ofFail(EMAIL_NOT_FOUND + email);
        }

        long orgId = adminOptional.get().getOrg().getId();
        try {
            FileHistoryTotalDto fileHistoryStatistics = fileHistoryStatisticsService.eventStatistics(orgId);
            return ResponseDto.ofSuccess(fileHistoryStatistics);
        } catch (RuntimeException e) {
            return ResponseDto.ofFail(e.getMessage());
        }
    }

    @PostMapping("/history/visualize")
    @ValidateJWT
    public ResponseDto<FileHistoryBySaaS> fileHistoryVisualize(@RequestBody EventIdRequest eventIdRequest, HttpServletRequest servletRequest){
        if (servletRequest.getAttribute(ERROR) != null) {
            String errorMessage = (String) servletRequest.getAttribute(ERROR);
            return ResponseDto.ofFail(errorMessage);
        }
        String email = (String) servletRequest.getAttribute(EMAIL);

        if (email == null) {
            return ResponseDto.ofFail(INVALID_JWT_MSG);
        }

        Optional<AdminUsers> adminOptional = adminRepo.findByEmail(email);
        if (adminOptional.isEmpty()) {
            return ResponseDto.ofFail(EMAIL_NOT_FOUND + email);
        }

        long orgId = adminOptional.get().getOrg().getId();
        long eventId = eventIdRequest.getEventId();

        if (activitiesRepo.findOrgIdByActivityId(eventId).equals(orgId)) {
            try {
                FileHistoryBySaaS fileHistoryBySaaS = fileVisualizeTestService.getFileHistoryBySaaS(eventId, orgId);
                return ResponseDto.ofSuccess(fileHistoryBySaaS);
            } catch (RuntimeException e) {
                return ResponseDto.ofFail(e.getMessage());
            }
        } else {
            return ResponseDto.ofFail("Invalid Request.");
        }
    }

    @GetMapping("/scan")
    @ValidateJWT
    public ResponseDto<FileListResponse> getFileList(HttpServletRequest servletRequest) {
        if (servletRequest.getAttribute(ERROR) != null) {
            String errorMessage = (String) servletRequest.getAttribute(ERROR);
            return ResponseDto.ofFail(errorMessage);
        }
        String email = (String) servletRequest.getAttribute(EMAIL);

        if (email == null) {
            return ResponseDto.ofFail(INVALID_JWT_MSG);
        }

        Optional<AdminUsers> adminOptional = adminRepo.findByEmail(email);
        if (adminOptional.isEmpty()) {
            return ResponseDto.ofFail(EMAIL_NOT_FOUND + email);
        }

        long orgId = adminOptional.get().getOrg().getId();
        try {
            FileListResponse fileListResponse = testService.getFileList(orgId);
            return ResponseDto.ofSuccess(fileListResponse);
        } catch (RuntimeException e) {
            return ResponseDto.ofFail(e.getMessage());
        }
    }
}

