package com.GASB.file.repository.file;

import com.GASB.file.model.entity.DlpStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DlpStatRepo extends JpaRepository<DlpStat, Long> {
}
