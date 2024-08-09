package com.GASB.file.model.dto.response.history;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class FileHistoryCorrelation {
    private long eventId;
    private String saas;
    private String eventType;
    private String fileName;
    private String saasFileId;
    private LocalDateTime eventTs;
    private String email;
    private String uploadChannel;

    @Builder
    public FileHistoryCorrelation(long eventId, String saas, String eventType, String fileName, String saasFileId, LocalDateTime eventTs, String email, String uploadChannel){
        this.eventId = eventId;
        this.saas = saas;
        this.eventType = eventType;
        this.fileName = fileName;
        this.saasFileId = saasFileId;
        this.eventTs = eventTs;
        this.email = email;
        this.uploadChannel = uploadChannel;
    }
}
