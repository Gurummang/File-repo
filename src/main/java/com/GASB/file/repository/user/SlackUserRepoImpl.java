package com.GASB.file.repository.user;

import com.GASB.file.model.entity.MonitoredUsers;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.List;

public class SlackUserRepoImpl implements SlackUserIF{
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void saveAllUsers(List<MonitoredUsers> users) {
        users.forEach(entityManager::persist);
    }
}
