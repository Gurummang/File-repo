package com.GASB.file.repository.user;

import com.GASB.file.model.entity.MonitoredUsers;
import com.GASB.file.model.entity.OrgSaaS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SlackUserRepo extends JpaRepository<MonitoredUsers, Long> , SlackUserIF{
    Optional<MonitoredUsers> findByUserId(String userId);
    String findUserNameByUserId(String userId);

    List<MonitoredUsers> findByOrgSaaS(OrgSaaS orgSaaS);

    boolean existsByUserId(String userId);
    // Other methods
}