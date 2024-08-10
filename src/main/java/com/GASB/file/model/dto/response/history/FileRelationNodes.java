package com.GASB.file.model.dto.response.history;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileRelationNodes {
    private long eventId;
    private String saas;
    private String eventType;
    private String fileName;
    private String hash256;
    private String saasFileId;
    private LocalDateTime eventTs;
    private String email;
    private String uploadChannel;
}
