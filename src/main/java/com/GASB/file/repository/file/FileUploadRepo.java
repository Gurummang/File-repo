package com.GASB.file.repository.file;

import com.GASB.file.model.dto.response.dashboard.TotalTypeDto;
import com.GASB.file.model.entity.FileUpload;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileUploadRepo extends JpaRepository<FileUpload, Long> {

    @Query("SELECT f.hash FROM FileUpload f WHERE f.saasFileId = :saasFileId")
    String findHashBySaasFileId(@Param("saasFileId") String saasFileId);

    @Query("SELECT fu FROM FileUpload fu " +
            "JOIN OrgSaaS os ON fu.orgSaaS.id = os.id " +
            "WHERE fu.deleted = false AND os.org.id = :orgId")
    List<FileUpload> findAllByOrgId(@Param("orgId") long orgId);

    @Query("SELECT COUNT(fu.id) FROM FileUpload fu JOIN OrgSaaS os ON fu.orgSaaS.id = os.id WHERE fu.deleted = false AND os.org.id = :orgId")
    Long countFileByOrgId(@Param("orgId") Long orgId);

    @Query("SELECT SUM(sf.size) FROM FileUpload fu " +
            "JOIN StoredFile sf ON fu.hash = sf.saltedHash " +
            "JOIN OrgSaaS os ON fu.orgSaaS.id = os.id " +
            "WHERE fu.deleted = false AND os.org.id = :orgId")
    Long getTotalSizeByOrgId(@Param("orgId") long orgId);

    @Query("SELECT COUNT(fu) " +
            "FROM FileUpload fu " +
            "JOIN fu.orgSaaS os " +
            "LEFT JOIN fu.storedFile sf " +
            "LEFT JOIN sf.vtReport vr " +
            "WHERE fu.deleted = false AND vr.threatLabel != 'none' AND os.org.id = :orgId")
    int countVtMalwareByOrgId(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(fu) " +
            "FROM FileUpload fu " +
            "JOIN fu.orgSaaS os " +
            "LEFT JOIN fu.storedFile sf " +
            "LEFT JOIN fu.typeScan ts " +
            "LEFT JOIN sf.scanTable gs " +
            "LEFT JOIN sf.vtReport vr " +
            "WHERE fu.deleted = false " +
            "  AND (vr IS NULL AND (ts.correct = false OR gs.detected = true)) " +
            "  AND os.org.id = :orgId")
    int countSuspiciousMalwareByOrgId(@Param("orgId") Long orgId);

    @Query("SELECT COUNT(DISTINCT fu) " +
            "FROM FileUpload fu " +
            "JOIN fu.orgSaaS os " +
            "JOIN fu.storedFile sf " +
            "JOIN sf.dlpReport dr " +
            "WHERE fu.deleted = false "+
            "AND sf.id IN ( " +
            "   SELECT DISTINCT d.storedFile.id " +
            "   FROM DlpReport d " +
            "   WHERE d.infoCnt >= 1 " +
            "   AND d.policy.orgSaaS.org.id = :orgId " +
            ") " +
            "AND os.org.id = :orgId")
    int countDlpIssuesByOrgId(@Param("orgId") Long orgId);

    @Query("SELECT new com.GASB.file.model.dto.response.dashboard.TotalTypeDto(sf.type, COUNT(sf)) " +
            "FROM FileUpload fu " +
            "JOIN fu.storedFile sf " +
            "JOIN fu.orgSaaS os " +
            "WHERE os.org.id = :orgId AND fu.deleted = false " +
            "GROUP BY sf.type")
    List<TotalTypeDto> findFileTypeDistributionByOrgId(@Param("orgId") Long orgId);

    @Query("SELECT fu.timestamp AS date, SUM(sf.size) AS totalSize, COUNT(fu) AS fileCount " +
            "FROM FileUpload fu " +
            "JOIN fu.storedFile sf " +
            "JOIN fu.orgSaaS os " +
            "WHERE os.org.id = :orgId AND fu.timestamp BETWEEN :startDate AND :endDate " +
            "GROUP BY fu.timestamp")
    List<Object[]> findStatistics(
            @Param("orgId") long orgId,
            @Param("startDate") LocalDateTime startDateTime,
            @Param("endDate") LocalDateTime endDateTime
    );

    @Query("SELECT MIN(f.timestamp) FROM FileUpload f WHERE f.orgSaaS.id = :orgSaaSId AND f.saasFileId = :saasFileId")
    LocalDateTime findEarliestUploadTsByOrgSaaS_IdAndSaasFileId(@Param("orgSaaSId") long orgSaaSId, @Param("saasFileId") String saasFileId);

    // 특정 orgSaaSId와 saasFileId에 대해 가장 최근의 hash를 가져오는 쿼리
    @Query("SELECT f.hash FROM FileUpload f WHERE f.orgSaaS.id = :orgSaaSId AND f.saasFileId = :saasFileId AND f.timestamp = (SELECT MAX(f2.timestamp) FROM FileUpload f2 WHERE f2.orgSaaS.id = :orgSaaSId AND f2.saasFileId = :saasFileId)")
    String findLatestHashBySaasFileId(@Param("orgSaaSId") long orgSaaSId, @Param("saasFileId") String saasFileId);

    @Query("SELECT f.hash FROM FileUpload f WHERE f.orgSaaS.id = :orgSaaSId AND f.saasFileId = :saasFileId AND f.timestamp = :eventTs")
    String findHashByOrgSaaS_IdAndSaasFileId(@Param("orgSaaSId") long orgSaaSId, @Param("saasFileId") String saasFileId, @Param("eventTs") LocalDateTime eventTs);

    @EntityGraph(attributePaths = {"storedFile.dlpReport"})
    @Query("SELECT f FROM FileUpload f WHERE f.id = :uploadId")
    Optional<FileUpload> findByIdWithDlpReport(@Param("uploadId") long uploadId);

    @Query("SELECT f.id FROM FileUpload f WHERE f.saasFileId=:saasFileId AND f.timestamp = :timestamp")
    Long findIdByActivities(@Param("saasFileId")String saasFileId, @Param("timestamp") LocalDateTime timestamp);
}
