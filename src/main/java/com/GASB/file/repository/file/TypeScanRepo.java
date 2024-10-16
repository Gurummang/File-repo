package com.GASB.file.repository.file;

import com.GASB.file.model.entity.TypeScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TypeScanRepo extends JpaRepository<TypeScan, Long> {

    @Query("SELECT t FROM TypeScan t WHERE t.fileUpload.id IN :fileUploadIds")
    List<TypeScan> findByFileUploadIds(@Param("fileUploadIds") List<Long> fileUploadIds);

}
