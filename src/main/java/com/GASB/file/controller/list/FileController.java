package com.GASB.file.controller.list;

import com.GASB.file.annotation.JWT.ValidateJWT;
import com.GASB.file.model.dto.request.EventIdRequest;
import com.GASB.file.model.dto.response.dashboard.FileDashboardDto;
import com.GASB.file.model.dto.response.history.*;
import com.GASB.file.model.dto.response.list.FileListResponse;
import com.GASB.file.model.dto.response.list.ResponseDto;
import com.GASB.file.model.entity.AdminUsers;
import com.GASB.file.repository.org.AdminRepo;
import com.GASB.file.service.dashboard.FileBoardReturnService;
import com.GASB.file.service.filescan.FileScanListService;
import com.GASB.file.service.history.FileHistoryService;
import com.GASB.file.service.history.FileHistoryStatisticsService;
import com.GASB.file.service.history.FileVisualizeTestService;
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
    private final FileVisualizeTestService fileVisualizeTestService;
    private final FileScanListService fileScanListService;
    private final AdminRepo adminRepo;
    private static final String INVALID_JWT_MSG = "Invalid JWT: email attribute is missing.";
    private static final String ERROR = "error";
    private static final String EMAIL = "email";
    private static final String EMAIL_NOT_FOUND = "Admin not found with email: ";

    @Autowired
    public FileController(FileBoardReturnService fileBoardReturnService, FileHistoryService fileHistoryService, FileHistoryStatisticsService fileHistoryStatisticsService,
                          FileVisualizeTestService fileVisualizeTestService, FileScanListService fileScanListService, AdminRepo adminRepo){
        this.fileBoardReturnService = fileBoardReturnService;
        this.fileHistoryService = fileHistoryService;
        this.fileHistoryStatisticsService = fileHistoryStatisticsService;
        this.fileVisualizeTestService = fileVisualizeTestService;
        this.fileScanListService = fileScanListService;
        this.adminRepo = adminRepo;
    }
    @GetMapping
    public String hello(){
        return "Hello, file world !!";
    }

    @GetMapping("/board")
    @ValidateJWT
    public ResponseDto<FileDashboardDto> fileDashboardList(HttpServletRequest servletRequest){
        try {
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
            FileDashboardDto fileDashboard = fileBoardReturnService.boardListReturn(orgId);
            return ResponseDto.ofSuccess(fileDashboard);
        } catch (Exception e) {
            return ResponseDto.ofFail(e.getMessage());
    }
}

    @GetMapping("/history")
    @ValidateJWT
    public ResponseDto<List<FileHistoryListDto>> fileHistoryList(HttpServletRequest servletRequest) {
        try {
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
            List<FileHistoryListDto> fileHistory = fileHistoryService.historyListReturn(orgId);
            return ResponseDto.ofSuccess(fileHistory);
        } catch (Exception e) {
            return ResponseDto.ofFail(e.getMessage());
        }
    }

    @GetMapping("/history/statistics")
    @ValidateJWT
    public ResponseDto<FileHistoryTotalDto> fileHistoryStatisticsList(HttpServletRequest servletRequest){
        try {
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
            FileHistoryTotalDto fileHistoryStatistics = fileHistoryStatisticsService.eventStatistics(orgId);
            return ResponseDto.ofSuccess(fileHistoryStatistics);
        } catch (Exception e){
            return ResponseDto.ofFail(e.getMessage());
        }
    }

    @PostMapping("/history/visualize")
    @ValidateJWT
    public ResponseDto<FileHistoryBySaaS> fileHistoryVisualize(@RequestBody EventIdRequest eventIdRequest, HttpServletRequest servletRequest){
        try {
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
            FileHistoryBySaaS fileHistoryBySaaS = fileVisualizeTestService.getFileHistoryBySaaS(eventId, orgId);
            return ResponseDto.ofSuccess(fileHistoryBySaaS);
        } catch (Exception e) {
            return ResponseDto.ofFail(e.getMessage());
        }
    }

    @GetMapping("/scan")
    @ValidateJWT
    public ResponseDto<FileListResponse> getFileList(HttpServletRequest servletRequest) {
        try {
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
            FileListResponse fileListResponse = fileScanListService.getFileList(orgId);
            return ResponseDto.ofSuccess(fileListResponse);
        } catch (Exception e){
            return ResponseDto.ofFail(e.getMessage());
        }
    }
}
