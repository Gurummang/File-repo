package com.GASB.file.controller.list;

import com.GASB.file.model.dto.response.dashboard.FileDashboardDto;
import com.GASB.file.model.dto.response.list.ResponseDto;
import com.GASB.file.service.dashboard.FileBoardReturnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileBoardReturnService fileBoardReturnService;

    @Autowired
    public FileController(FileBoardReturnService fileBoardReturnService){
        this.fileBoardReturnService = fileBoardReturnService;
    }
    @GetMapping
    public String hello(){
        return "Hello, file world !!";
    }

    @PostMapping("/board")
    public ResponseDto<FileDashboardDto> fileDashboardList(@RequestBody long org_saas_id){
        FileDashboardDto fileDashboard = fileBoardReturnService.boardListReturn(org_saas_id);
        return ResponseDto.ofSuccess(fileDashboard);
    }
}
