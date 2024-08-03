package com.GASB.file.repository.user;
import com.GASB.file.model.entity.MonitoredUsers;

import java.util.List;
public interface SlackUserIF {
    void saveAllUsers(List<MonitoredUsers> users);
}
