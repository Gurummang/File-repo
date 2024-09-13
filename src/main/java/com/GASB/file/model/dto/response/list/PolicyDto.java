package com.GASB.file.model.dto.response.list;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PolicyDto {

    private String policyName;
    private String description;

}
