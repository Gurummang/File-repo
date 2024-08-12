package com.GASB.file.model.dto.response.list;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class MimeTypeDto {

    private boolean correct;
    private String mimeType;
    private String signature;
    private String extension;

    public MimeTypeDto(boolean correct, String mimeType, String signature, String extension) {
        this.correct = correct;
        this.mimeType = mimeType;
        this.signature = signature;
        this.extension = extension;
    }
}
