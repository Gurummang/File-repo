package com.GASB.file.repository.file;

import com.GASB.file.model.entity.DlpReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface DlpReportRepo extends JpaRepository<DlpReport, Long> {

    @Query("SELECT d FROM DlpReport d JOIN d.storedFile s WHERE s.id = :storedId AND d.policy.orgSaaS.org.id = :orgId")
    List<DlpReport> findDlpReportsByUploadIdAndOrgId(@Param("storedId") long storedId, @Param("orgId") long orgId);


    @Query("SELECT d FROM DlpReport d JOIN d.storedFile s WHERE d.policy.orgSaaS.org.id = :orgId")
    List<DlpReport> findAllDlpReportsByOrgId(@Param("orgId") long orgId);

    @Query("""
        SELECT dr FROM DlpReport dr
        JOIN FETCH dr.policy p
        JOIN FETCH dr.pii
        JOIN FETCH dr.storedFile sf
        WHERE sf.id IN :storedFileIds
    """)
    List<DlpReport> findAllByStoredFileIds(@Param("storedFileIds") Set<Long> storedFileIds);

}
