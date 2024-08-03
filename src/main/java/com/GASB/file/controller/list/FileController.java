package com.GASB.file.controller.list;

import com.GASB.file.model.dto.response.dashboard.FileDashboardDto;
import com.GASB.file.model.dto.response.list.ResponseDto;
import com.GASB.file.service.dashboard.FileBoardReturnService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/board")
    public ResponseDto<FileDashboardDto> fileDashboardList(){
        FileDashboardDto fileDashboard = fileBoardReturnService.boardListReturn();
        return ResponseDto.ofSuccess(fileDashboard);
    }
}
