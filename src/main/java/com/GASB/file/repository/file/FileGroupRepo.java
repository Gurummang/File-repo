package com.GASB.file.repository.file;

import com.GASB.file.model.entity.FileGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FileGroupRepo extends JpaRepository<FileGroup, Long>{

    @Query("SELECT fg.groupName FROM FileGroup fg WHERE fg.id = :activityId")
    String findGroupNameById(@Param("activityId") long activityId);
}
