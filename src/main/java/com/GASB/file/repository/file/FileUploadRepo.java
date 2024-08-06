package com.GASB.file.repository.file;

import com.GASB.file.model.dto.response.dashboard.TotalTypeDto;
import com.GASB.file.model.entity.FileUpload;
import com.GASB.file.model.entity.OrgSaaS;
import com.GASB.file.model.entity.SaaS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileUploadRepo extends JpaRepository<FileUpload, Long> {
//    Optional<FileUpload> findBySaasFileId(String saasFileId);
//    List<FileUpload> findTop10ByOrderByTimestampDesc();
//    List<FileUpload> findByOrgSaaS(OrgSaaS orgSaaS);
//    List<FileUpload> findByOrgSaaSInOrderByTimestampDesc(List<OrgSaaS> orgSaaSList);
//    List<FileUpload> findTop10ByOrgSaaSInOrderByTimestampDesc(List<OrgSaaS> orgSaasList);

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

    @Query("SELECT COUNT(fu) " +
            "FROM FileUpload fu " +
            "JOIN fu.orgSaaS os " +
            "JOIN fu.storedFile sf " +
            "JOIN sf.dlpReport dr " +
            "WHERE fu.deleted = false AND dr.dlp = true AND os.org.id = :orgId")
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


    // Corrected method to find by OrgSaaS fields

//    Optional<FileUpload> findBySaasFileIdAndTimestamp(String saasFileId, LocalDateTime timestamp);
//    List<FileUpload> findTop10ByOrgSaaS_Org_IdAndOrgSaaS_SaasOrderByTimestampDesc(int orgId, SaaS saas);
}
