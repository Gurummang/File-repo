package com.GASB.file.model.dto.response.history;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileHistoryDto {
    private long eventId;
    private String saas;
    private String eventType;
    private String fileName;
    private String saasFileId;
    private LocalDateTime uploadTs;
    private LocalDateTime eventTs;
    private String email;
    private String uploadChannel;
}