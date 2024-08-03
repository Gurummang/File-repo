package com.GASB.file.repository.file;

import com.GASB.file.model.entity.StoredFile;
import com.GASB.file.model.entity.VtReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VtReportRepo extends JpaRepository<VtReport, Long> {
    Optional<VtReport> findByStoredFile(StoredFile storedFile);
}
