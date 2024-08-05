package com.GASB.file.repository.file;

import com.GASB.file.model.entity.FileUpload;
import com.GASB.file.model.entity.OrgSaaS;
import com.GASB.file.model.entity.SaaS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    @Query("SELECT COUNT(f) FROM FileUpload f WHERE f.orgSaaS.id = :orgSaasId")
    int countByOrgSaasId(@Param("orgSaasId") Long orgSaasId);

    @Query("SELECT SUM(sf.size) FROM FileUpload fu JOIN fu.storedFile sf WHERE fu.orgSaaS.id = :orgSaasId")
    double sumFileSizeByOrgSaasId(@Param("orgSaasId") Long orgSaasId);

    @Query("SELECT COUNT(fu) " +
            "FROM FileUpload fu " +
            "JOIN fu.storedFile sf " +
            "JOIN sf.vtReport vr " +
            "WHERE vr.threatLabel != 'none' AND fu.orgSaaS.id = :orgSaasId")
    int countVtReportsByOrgSaasId(@Param("orgSaasId") Long orgSaasId);

    @Query("SELECT COUNT(fu) " +
            "FROM FileUpload fu " +
            "JOIN fu.storedFile sf " +
            "JOIN sf.dlpReport dr " +
            "WHERE dr.dlp = true AND fu.orgSaaS.id = :orgSaasId")
    int countDlpReportsByOrgSaasId(@Param("orgSaasId") Long orgSaasId);

    // Corrected method to find by OrgSaaS fields

//    Optional<FileUpload> findBySaasFileIdAndTimestamp(String saasFileId, LocalDateTime timestamp);
//    List<FileUpload> findTop10ByOrgSaaS_Org_IdAndOrgSaaS_SaasOrderByTimestampDesc(int orgId, SaaS saas);
}
