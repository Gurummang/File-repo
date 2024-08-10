package com.GASB.file.model.dto.response.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileRelation {
    private List<FileRelationNodes> nodes;
    private List<FileRelationEdges> edges;
}
