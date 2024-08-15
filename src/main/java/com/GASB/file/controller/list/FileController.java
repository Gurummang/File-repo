package com.GASB.file.controller.list;

import com.GASB.file.model.dto.request.EventIdRequest;
import com.GASB.file.model.dto.request.OrgIdRequest;
import com.GASB.file.model.dto.response.dashboard.FileDashboardDto;
import com.GASB.file.model.dto.response.history.*;
import com.GASB.file.model.dto.response.list.FileListResponse;
import com.GASB.file.model.dto.response.list.ResponseDto;
import com.GASB.file.service.dashboard.FileBoardReturnService;
import com.GASB.file.service.filescan.FileScanListService;
import com.GASB.file.service.history.FileHistoryService;
import com.GASB.file.service.history.FileHistoryStatisticsService;
import com.GASB.file.service.history.FileVisualizeService;
import com.GASB.file.service.history.FileVisualizeTestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileBoardReturnService fileBoardReturnService;
    private final FileHistoryService fileHistoryService;
    private final FileHistoryStatisticsService fileHistoryStatisticsService;
    private final FileVisualizeService fileVisualizeService;
    private final FileVisualizeTestService fileVisualizeTestService;
    private final FileScanListService fileScanListService;

    @Autowired
    public FileController(FileBoardReturnService fileBoardReturnService, FileHistoryService fileHistoryService, FileHistoryStatisticsService fileHistoryStatisticsService, FileVisualizeService fileVisualizeService,
                          FileVisualizeTestService fileVisualizeTestService, FileScanListService fileScanListService){
        this.fileBoardReturnService = fileBoardReturnService;
        this.fileHistoryService = fileHistoryService;
        this.fileHistoryStatisticsService = fileHistoryStatisticsService;
        this.fileVisualizeService = fileVisualizeService;
        this.fileVisualizeTestService = fileVisualizeTestService;
        this.fileScanListService = fileScanListService;
    }
    @GetMapping
    public String hello(){
        return "Hello, file world !!";
    }

    @PostMapping("/board")
    public ResponseDto<FileDashboardDto> fileDashboardList(@RequestBody OrgIdRequest orgIdRequest){
        long orgId = orgIdRequest.getOrgId();
        FileDashboardDto fileDashboard = fileBoardReturnService.boardListReturn(orgId);
        return ResponseDto.ofSuccess(fileDashboard);
    }

    @PostMapping("/history")
    public ResponseDto<List<FileHistoryListDto>> fileHistoryList(@RequestBody OrgIdRequest orgIdRequest){
        long orgId = orgIdRequest.getOrgId();
        List<FileHistoryListDto> fileHistory = fileHistoryService.historyListReturn(orgId);
        return ResponseDto.ofSuccess(fileHistory);
    }

    @PostMapping("/history/statistics")
    public ResponseDto<FileHistoryTotalDto> fileHistoryStatisticsList(@RequestBody OrgIdRequest orgIdRequest){
        long orgId = orgIdRequest.getOrgId();
        FileHistoryTotalDto fileHistoryStatistics = fileHistoryStatisticsService.eventStatistics(orgId);
        return ResponseDto.ofSuccess(fileHistoryStatistics);
    }

    @PostMapping("/history/visualize")
    public ResponseDto<FileHistoryBySaaS> fileHistoryVisualize(@RequestBody EventIdRequest eventIdRequest){
        long eventId = eventIdRequest.getEventId();
        FileHistoryBySaaS fileHistoryBySaaS = fileVisualizeTestService.getFileHistoryBySaaS(eventId);
        return ResponseDto.ofSuccess(fileHistoryBySaaS);
    }

    @PostMapping("/scan")
    public ResponseDto<FileListResponse> getFileList(@RequestBody OrgIdRequest orgIdRequest) {
        try {
            long orgId = orgIdRequest.getOrgId();
            FileListResponse fileListResponse = fileScanListService.getFileList(orgId);
            return ResponseDto.ofSuccess(fileListResponse);
        } catch (Exception e){
            return ResponseDto.ofFail(e.getMessage());
        }
    }
}
