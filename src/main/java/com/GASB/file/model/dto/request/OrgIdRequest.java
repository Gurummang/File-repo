package com.GASB.file.model.dto.request;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class OrgIdRequest {
        private long orgId;

        public OrgIdRequest(long orgId){
                this.orgId = orgId;
        }
}
