package com.GASB.file.repository.file;

import com.GASB.file.model.entity.FileStatus;
import com.GASB.file.model.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileStatusRepo extends JpaRepository<FileStatus, Long> {
    FileStatus findByStoredFile(StoredFile storedFile);
}
