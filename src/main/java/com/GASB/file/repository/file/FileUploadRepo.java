package com.GASB.file.repository.file;

import com.GASB.file.model.entity.FileUpload;
import com.GASB.file.model.entity.OrgSaaS;
import com.GASB.file.model.entity.SaaS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileUploadRepo extends JpaRepository<FileUpload, Long> {
    Optional<FileUpload> findBySaasFileId(String saasFileId);
    List<FileUpload> findTop10ByOrderByTimestampDesc();
    List<FileUpload> findByOrgSaaS(OrgSaaS orgSaaS);
    List<FileUpload> findByOrgSaaSInOrderByTimestampDesc(List<OrgSaaS> orgSaaSList);
    List<FileUpload> findTop10ByOrgSaaSInOrderByTimestampDesc(List<OrgSaaS> orgSaasList);


    // Corrected method to find by OrgSaaS fields

    Optional<FileUpload> findBySaasFileIdAndTimestamp(String saasFileId, LocalDateTime timestamp);
    List<FileUpload> findTop10ByOrgSaaS_Org_IdAndOrgSaaS_SaasOrderByTimestampDesc(int orgId, SaaS saas);
}
