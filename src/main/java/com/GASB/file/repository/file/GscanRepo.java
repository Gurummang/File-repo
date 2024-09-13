package com.GASB.file.repository.file;

import com.GASB.file.model.entity.Gscan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GscanRepo extends JpaRepository<Gscan, Long> {

    @Query("SELECT g FROM Gscan g WHERE g.storedFile.saltedHash = :hash")
    Gscan findByHash(@Param("hash")String hash);
}
