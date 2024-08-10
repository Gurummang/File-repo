package com.GASB.file.controller.list;

import com.GASB.file.config.rabbitmq.RabbitMQProperties;
import com.GASB.file.model.dto.request.EventIdRequest;
import com.GASB.file.model.dto.request.OrgIdRequest;
import com.GASB.file.model.dto.response.dashboard.FileDashboardDto;
import com.GASB.file.model.dto.response.history.*;
import com.GASB.file.model.dto.response.list.ResponseDto;
import com.GASB.file.service.dashboard.FileBoardReturnService;
import com.GASB.file.service.history.FileHistoryService;
import com.GASB.file.service.history.FileHistoryStatisticsService;
import com.GASB.file.service.history.FileNodeService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileBoardReturnService fileBoardReturnService;
    private final FileHistoryService fileHistoryService;
    private final FileHistoryStatisticsService fileHistoryStatisticsService;
    private final FileNodeService fileNodeService;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties properties;

    @Autowired
    public FileController(FileBoardReturnService fileBoardReturnService, FileHistoryService fileHistoryService, FileHistoryStatisticsService fileHistoryStatisticsService, FileNodeService fileNodeService,
                          RabbitTemplate rabbitTemplate, RabbitMQProperties properties){
        this.fileBoardReturnService = fileBoardReturnService;
        this.fileHistoryService = fileHistoryService;
        this.fileHistoryStatisticsService = fileHistoryStatisticsService;
        this.fileNodeService = fileNodeService;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
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

    @GetMapping("/history")
    public ResponseDto<List<FileHistoryListDto>> fileHistoryList(@RequestBody OrgIdRequest orgIdRequest){
        long orgId = orgIdRequest.getOrgId();
        List<FileHistoryListDto> fileHistory = fileHistoryService.historyListReturn(orgId);
        // rabbitTemplate.convertAndSend(properties.getGroupingRoutingKey(),orgId);
        return ResponseDto.ofSuccess(fileHistory);
    }

    @PostMapping("/history")
    public ResponseDto<FileHistoryBySaaS> fileHistoryGroupReturn(@RequestBody EventIdRequest eventIdRequest){
        long eventId = eventIdRequest.getEventId();
        FileHistoryBySaaS fileHistoryCorrelationsInfo = fileHistoryService.createFileHistoryCorrelations(eventId);
        return ResponseDto.ofSuccess(fileHistoryCorrelationsInfo);
    }

    @GetMapping("/history/statistics")
    public ResponseDto<FileHistoryTotalDto> fileHistoryStatisticsList(@RequestBody OrgIdRequest orgIdRequest){
        long orgId = orgIdRequest.getOrgId();
        FileHistoryTotalDto fileHistoryStatistics = fileHistoryStatisticsService.eventStatistics(orgId);
        return ResponseDto.ofSuccess(fileHistoryStatistics);
    }

    @GetMapping("/history/visualize")
    public ResponseDto<FileRelation> fileHistoryVisualize(@RequestBody EventIdRequest eventIdRequest){
        long eventId = eventIdRequest.getEventId();
        FileRelation fileRelation = fileNodeService.returnFileRelation(eventId);
        return ResponseDto.ofSuccess(fileRelation);
    }
}
