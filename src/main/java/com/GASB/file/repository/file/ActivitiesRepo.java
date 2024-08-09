package com.GASB.file.repository.file;

import com.GASB.file.model.entity.Activities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivitiesRepo extends JpaRepository<Activities, Long> {
    Optional<Activities> findBysaasFileId(String fileId);

    @Query("SELECT DATE(av.eventTs) AS date, " +
            "SUM(CASE WHEN av.eventType = 'file_uploaded' THEN 1 ELSE 0 END) AS uploadCount, " +
            "SUM(CASE WHEN av.eventType = 'file_changed' THEN 1 ELSE 0 END) AS modifyCount, " +
            "SUM(CASE WHEN av.eventType = 'file_deleted' THEN 1 ELSE 0 END) AS deletedCount, " +
            "SUM(CASE WHEN av.eventType = 'file_moved' THEN 1 ELSE 0 END) AS movedCount " +
            "FROM Activities av " +
            "JOIN av.user mu " +
            "JOIN mu.orgSaaS os " +
            "WHERE os.org.id = :orgId AND av.eventTs BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(av.eventTs)")
    List<Object[]> findFileHistoryStatistics(
            @Param("orgId") long orgId,
            @Param("startDate") LocalDateTime startDateTime,
            @Param("endDate") LocalDateTime endDateTime
    );

    @Query("SELECT COUNT(av) " +
            "FROM Activities av " +
            "JOIN av.user mu " +
            "JOIN mu.orgSaaS os " +
            "WHERE os.org.id = :orgId AND av.eventType = 'file_uploaded'")
    int findTotalUploadCount(@Param("orgId") long orgId);

    @Query("SELECT COUNT(av) " +
            "FROM Activities av " +
            "JOIN av.user mu " +
            "JOIN mu.orgSaaS os " +
            "WHERE os.org.id = :orgId AND av.eventType = 'file_deleted'")
    int findTotalDeletedCount(@Param("orgId") long orgId);

    @Query("SELECT COUNT(av) " +
            "FROM Activities av " +
            "JOIN av.user mu " +
            "JOIN mu.orgSaaS os " +
            "WHERE os.org.id = :orgId AND av.eventType = 'file_changed'")
    int findTotalChangedCount(@Param("orgId") long orgId);

    @Query("SELECT COUNT(av) " +
            "FROM Activities av " +
            "JOIN av.user mu " +
            "JOIN mu.orgSaaS os " +
            "WHERE os.org.id = :orgId AND av.eventType = 'file_moved'")
    int findTotalMovedCount(@Param("orgId") long orgId);

}
