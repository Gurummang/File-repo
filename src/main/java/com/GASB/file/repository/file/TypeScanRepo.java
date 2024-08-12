package com.GASB.file.repository.file;

import com.GASB.file.model.entity.TypeScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TypeScanRepo extends JpaRepository<TypeScan, Long> {
    @Query("SELECT ts FROM TypeScan ts JOIN ts.fileUpload fu WHERE fu.hash = :hash")
    Optional<TypeScan> findByHash(@Param("hash") String hash);
}
