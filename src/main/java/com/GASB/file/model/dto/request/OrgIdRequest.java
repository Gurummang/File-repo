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
        private long org_id;

        public OrgIdRequest(long org_id){
                this.org_id = org_id;
        }
}
