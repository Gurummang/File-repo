package com.GASB.file.repository.file;

import com.GASB.file.model.entity.Activities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ActivitiesRepo extends JpaRepository<Activities, Long> {
    Optional<Activities> findBysaasFileId(String fileId);

    Optional<Activities> findBySaasFileIdAndEventTs(String fileId, LocalDateTime eventTs);
}
