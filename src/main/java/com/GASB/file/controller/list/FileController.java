package com.GASB.file.controller.list;

import com.GASB.file.config.rabbitmq.RabbitMQProperties;
import com.GASB.file.model.dto.request.OrgIdRequest;
import com.GASB.file.model.dto.response.dashboard.FileDashboardDto;
import com.GASB.file.model.dto.response.history.FileHistoryDto;
import com.GASB.file.model.dto.response.list.ResponseDto;
import com.GASB.file.service.dashboard.FileBoardReturnService;
import com.GASB.file.service.history.FileHistoryService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileBoardReturnService fileBoardReturnService;
    private final FileHistoryService fileHistoryService;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQProperties properties;

    @Autowired
    public FileController(FileBoardReturnService fileBoardReturnService, FileHistoryService fileHistoryService, RabbitTemplate rabbitTemplate, RabbitMQProperties properties){
        this.fileBoardReturnService = fileBoardReturnService;
        this.fileHistoryService = fileHistoryService;
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }
    @GetMapping
    public String hello(){
        return "Hello, file world !!";
    }

    @PostMapping("/board")
    public ResponseDto<FileDashboardDto> fileDashboardList(@RequestBody OrgIdRequest orgIdRequest){
        long org_id = orgIdRequest.getOrgId();
        FileDashboardDto fileDashboard = fileBoardReturnService.boardListReturn(org_id);
        return ResponseDto.ofSuccess(fileDashboard);
    }

    @GetMapping("/history")
    public ResponseDto<List<FileHistoryDto>> fileHistoryList(){
        //long orgId = orgIdRequest.getOrgId(); // @RequestBody OrgIdRequest orgIdRequest
        List<FileHistoryDto> fileHistory = fileHistoryService.historyListReturn();
        // rabbitTemplate.convertAndSend(properties.getGroupingRoutingKey(),orgId);
        return ResponseDto.ofSuccess(fileHistory);
    }
}
