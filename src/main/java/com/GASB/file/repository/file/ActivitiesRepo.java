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

    Optional<Activities> findBySaasFileId(String fileId);

    @Query("SELECT a FROM Activities a WHERE a.saasFileId = :saasFileId AND a.eventType = 'file_uploaded'")
    Activities getActivitiesBySaaSFileId(@Param("saasFileId") String saasFileId);

    @Query("SELECT a FROM Activities a WHERE a.user.orgSaaS.org.id = :orgId AND a.fileGroup.groupName = :groupName")
    List<Activities> findByOrgIdAndGroupName(@Param("orgId") long orgId, @Param("groupName") String groupName);

    @Query("SELECT a FROM Activities a WHERE a.saasFileId = :saasFileId")
    List<Activities> findListBySaasFileId(@Param("saasFileId") String saasFileId);

    List<Activities> findByUser_OrgSaaS_Org_Id(long orgId);

    @Query("SELECT a FROM Activities a WHERE a.saasFileId IN (SELECT f.saasFileId FROM FileUpload f WHERE f.hash = :hash) AND a.eventTs IN (SELECT f.timestamp FROM FileUpload f WHERE f.hash = :hash)")
    List<Activities> findByHash(@Param("hash") String hash);

    @Query("SELECT a.user.orgSaaS.org.id FROM Activities a WHERE a.id = :activityId")
    Long findOrgIdByActivityId(@Param("activityId") long id);

    @Query("SELECT a FROM Activities a " +
            "JOIN a.user u " +
            "JOIN u.orgSaaS os " +
            "WHERE os.org.id = :orgId " +
            "AND a.id IN (" +
            "SELECT fg.id FROM FileGroup fg WHERE fg.groupName = :groupName" +
            ")")
    List<Activities> findAllByOrgIdAndGroupName(
            @Param("orgId") long orgId,
            @Param("groupName") String groupName
    );

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
