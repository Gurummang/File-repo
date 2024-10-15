package com.GASB.file.repository.file;

import com.GASB.file.model.entity.Gscan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GscanRepo extends JpaRepository<Gscan, Long> {

    @Query("SELECT g FROM Gscan g WHERE g.storedFile.id IN :storedFileIds")
    List<Gscan> findByStoredFileIds(@Param("storedFileIds") List<Long> storedFileIds);
}
