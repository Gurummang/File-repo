package com.GASB.file.config.slack;

import com.GASB.file.annotation.SlackBoardGroup;
import com.GASB.file.annotation.SlackInitGroup;
import com.GASB.file.annotation.ValidEmail;
import com.GASB.file.annotation.ValidWorkspace;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ExtractData {

    @ValidWorkspace(message = "Invalid Workspace ID", groups = SlackInitGroup.class)
    @NotNull(groups = SlackInitGroup.class)
    private Integer workspace_config_id;

    @ValidEmail(message = "Invalid Email", groups = SlackBoardGroup.class)
    @NotNull(groups = SlackBoardGroup.class)
    private String email;
}
