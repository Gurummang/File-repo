package com.GASB.file.model.dto.response.history;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class FileHistoryBySaaS {
    private List<FileRelationNodes> slack;
    private List<FileRelationNodes> googleDrive;
    private List<FileRelationEdges> edges;

    public FileHistoryBySaaS(List<FileRelationNodes> slack, List<FileRelationNodes> googleDrive, List<FileRelationEdges> edges){
        this.slack = slack;
        this.googleDrive =googleDrive;
        this.edges = edges;
    }
}
