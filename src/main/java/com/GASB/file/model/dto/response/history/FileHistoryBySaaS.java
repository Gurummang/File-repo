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
    private List<FileRelationNodes> o365;
    private List<FileRelationEdges> edges;

    public FileHistoryBySaaS(long originNode, List<FileRelationNodes> slack, List<FileRelationNodes> googleDrive, List<FileRelationNodes> o365, List<FileRelationEdges> edges){
        this.originNode = originNode;
        this.slack = List.copyOf(slack);
        this.googleDrive = List.copyOf(googleDrive);
        this.o365 = List.copyOf(o365);
        this.edges = List.copyOf(edges);
    }
}
