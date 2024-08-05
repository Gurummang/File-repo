package com.GASB.file.repository.org;

import com.GASB.file.model.entity.OrgSaaS;
import com.GASB.file.model.entity.SaaS;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface OrgSaaSRepo extends JpaRepository<OrgSaaS, Integer> {
    Optional<OrgSaaS> findById(Long id);
    Optional<OrgSaaS> findBySpaceId(String spaceId);
    Optional<OrgSaaS> findByOrgIdAndSpaceId(int orgId, String spaceId);
    Optional<OrgSaaS> findByOrgIdAndSaas(int orgId, SaaS saas);
    List<OrgSaaS> findAllByOrgIdAndSaas(int orgId, SaaS saas);
    Optional<OrgSaaS> findBySpaceIdAndOrgId(String spaceId, int orgId);
    OrgSaaS findByOrgId(long orgId);
    boolean existsBySpaceId(String spaceId);

}
