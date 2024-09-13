package com.GASB.file.repository.file;

import com.GASB.file.model.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoredFileRepo extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findBySaltedHash(String saltedHash);

}
