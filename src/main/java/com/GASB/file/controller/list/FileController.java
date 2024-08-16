package com.GASB.file.controller.list;

import com.GASB.file.annotation.JWT.ValidateJWT;
import com.GASB.file.model.dto.request.EventIdRequest;
import com.GASB.file.model.dto.request.OrgIdRequest;
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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
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
        // JWT 검증 실패 여부를 확인
        if (servletRequest.getAttribute("error") != null) {
            String errorMessage = (String) servletRequest.getAttribute("error");
            return ResponseDto.ofFail(errorMessage);
        }
//        Object errorAttr = servletRequest.getAttribute("error");
//        if (errorAttr != null) {
//            String errorMessage = (String) errorAttr;
//            return ResponseDto.ofFail(errorMessage);  // 에러 메시지 반환
//        }

        try {
            String email = (String) servletRequest.getAttribute("email");

            // email 속성이 null인 경우 처리
            if (email == null) {
                return ResponseDto.ofFail("Invalid JWT: email attribute is missing.");
            }

            Optional<AdminUsers> adminOptional = adminRepo.findByEmail(email);
            if (adminOptional.isEmpty()) {
                return ResponseDto.ofFail("Admin not found with email: " + email);
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
        // JWT 검증 실패 여부를 확인
//        Object errorAttr = servletRequest.getAttribute("error");
//        if (errorAttr != null) {
//            String errorMessage = (String) errorAttr;
//            return ResponseDto.ofFail(errorMessage);  // 에러 메시지 반환
//        }

        try {
            if (servletRequest.getAttribute("error") != null) {
                String errorMessage = (String) servletRequest.getAttribute("error");
                return ResponseDto.ofFail(errorMessage);
            }
            String email = (String) servletRequest.getAttribute("email");

            // email 속성이 null인 경우 처리
            if (email == null) {
                return ResponseDto.ofFail("Invalid JWT: email attribute is missing.");
            }

            Optional<AdminUsers> adminOptional = adminRepo.findByEmail(email);
            if (adminOptional.isEmpty()) {
                return ResponseDto.ofFail("Admin not found with email: " + email);
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
        // JWT 검증 실패 여부를 확인
        Object errorAttr = servletRequest.getAttribute("error");
        if (errorAttr != null) {
            String errorMessage = (String) errorAttr;
            return ResponseDto.ofFail(errorMessage);  // 에러 메시지 반환
        }

        try {
            String email = (String) servletRequest.getAttribute("email");

            // email 속성이 null인 경우 처리
            if (email == null) {
                return ResponseDto.ofFail("Invalid JWT: email attribute is missing.");
            }

            Optional<AdminUsers> adminOptional = adminRepo.findByEmail(email);
            if (adminOptional.isEmpty()) {
                return ResponseDto.ofFail("Admin not found with email: " + email);
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
        // JWT 검증 실패 여부를 확인
        Object errorAttr = servletRequest.getAttribute("error");
        if (errorAttr != null) {
            String errorMessage = (String) errorAttr;
            return ResponseDto.ofFail(errorMessage);  // 에러 메시지 반환
        }

        try {
            String email = (String) servletRequest.getAttribute("email");

            // email 속성이 null인 경우 처리
            if (email == null) {
                return ResponseDto.ofFail("Invalid JWT: email attribute is missing.");
            }

            Optional<AdminUsers> adminOptional = adminRepo.findByEmail(email);
            if (adminOptional.isEmpty()) {
                return ResponseDto.ofFail("Admin not found with email: " + email);
            }

            long orgId = adminOptional.get().getOrg().getId();
            long eventId = eventIdRequest.getEventId();
            FileHistoryBySaaS fileHistoryBySaaS = fileVisualizeTestService.getFileHistoryBySaaS(eventId, orgId);
            return ResponseDto.ofSuccess(fileHistoryBySaaS);
        } catch (Exception e) {
            return ResponseDto.ofFail(e.getMessage());
        }
    }


    @PostMapping("/scan")
//    @ValidateJWT
    public ResponseDto<FileListResponse> getFileList(@RequestBody OrgIdRequest orgIdRequest) {
        try {
//            if (servletRequest.getAttribute("error") != null) {
//                String errorMessage = (String) servletRequest.getAttribute("error");
//                return ResponseDto.ofFail(errorMessage);
//            }
//            String email = (String) servletRequest.getAttribute("email");
//            long orgId = adminRepo.findByEmail(email).get().getOrg().getId();
            long orgId = orgIdRequest.getOrgId();
            FileListResponse fileListResponse = fileScanListService.getFileList(orgId);
            return ResponseDto.ofSuccess(fileListResponse);
        } catch (Exception e){
            return ResponseDto.ofFail(e.getMessage());
        }
    }

    @GetMapping("/cookies")
    public String checkCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "No cookies found";
        }

        for (Cookie cookie : cookies) {
            System.out.println("Cookie Name: " + cookie.getName());
            System.out.println("Cookie Value: " + cookie.getValue());
        }

        return "Cookies checked, see server logs";
    }
}
