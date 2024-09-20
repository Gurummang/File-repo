package com.GASB.file.repository.file;

import com.GASB.file.model.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyRepo extends JpaRepository<Policy, Long> {

    @Query("SELECT p FROM Policy p WHERE p.orgSaaS.org.id = :orgId")
    List<Policy> findPoliciesByOrgId(@Param("orgId") long orgId);

}
