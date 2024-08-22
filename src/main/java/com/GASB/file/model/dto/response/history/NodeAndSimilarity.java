package com.GASB.file.model.dto.response.history;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class NodeAndSimilarity {
    private FileRelationNodes fileRelationNodes;
    private String saasName;

    public NodeAndSimilarity(FileRelationNodes fileRelationNodes, String saasName) {
        this.fileRelationNodes = fileRelationNodes;
        this.saasName = saasName;
    }
}

