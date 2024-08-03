package com.GASB.file.repository.org;

import com.GASB.file.model.entity.WorkspaceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkspaceConfigRepo extends JpaRepository<WorkspaceConfig, String> {
    Optional<WorkspaceConfig> findById(int id);
    boolean existsById(int id);
}
