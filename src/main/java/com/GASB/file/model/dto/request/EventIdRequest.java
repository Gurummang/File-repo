package com.GASB.file.model.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class EventIdRequest {
    private long eventId;

    public EventIdRequest(long eventId){
        this.eventId = eventId;
    }
}
