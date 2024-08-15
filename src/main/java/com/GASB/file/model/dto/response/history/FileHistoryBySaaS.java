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
    private long originNode;
    private List<FileRelationNodes> slack;
    private List<FileRelationNodes> googleDrive;
    private List<FileRelationEdges> edges;

    public FileHistoryBySaaS(long originNode, List<FileRelationNodes> slack, List<FileRelationNodes> googleDrive, List<FileRelationEdges> edges){
        this.originNode = originNode;
        this.slack = List.copyOf(slack);
        this.googleDrive = List.copyOf(googleDrive);
        this.edges = List.copyOf(edges);
    }
}
