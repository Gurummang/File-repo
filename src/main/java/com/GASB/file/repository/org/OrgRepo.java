package com.GASB.file.repository.org;

import com.GASB.file.model.entity.AdminUsers;
import com.GASB.file.model.entity.Org;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgRepo extends JpaRepository<Org, Integer> {
}
