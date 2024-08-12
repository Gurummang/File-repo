package com.GASB.file.model.dto.response.list;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileListResponse {
    private int total;
    private int dlpTotal;
    private int malwareTotal;
    private List<FileListDto> files;

    public static FileListResponse of(int total, int dlpTotal, int malwareTotal, List<FileListDto> files) {
        return FileListResponse.builder()
                .total(total)
                .dlpTotal(dlpTotal)
                .malwareTotal(malwareTotal)
                .files(files)
                .build();
    }
}