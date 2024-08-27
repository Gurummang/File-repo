package com.GASB.file.model.dto.response.history;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Builder
public class NodeAndSimilarity {
    private List<FileRelationNodes> slackNodes;
    private List<FileRelationNodes> googleDriveNodes;
    private List<FileRelationNodes> o365Nodes;

    public NodeAndSimilarity(List<FileRelationNodes> slackNodes, List<FileRelationNodes> googleDriveNodes, List<FileRelationNodes> o365Nodes) {
        this.slackNodes = slackNodes;
        this.googleDriveNodes = googleDriveNodes;
        this.o365Nodes = o365Nodes;
    }
}

