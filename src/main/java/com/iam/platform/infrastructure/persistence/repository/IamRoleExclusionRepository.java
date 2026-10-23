package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamRoleExclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IamRoleExclusionRepository extends JpaRepository<IamRoleExclusion, Long> {
    List<IamRoleExclusion> findByRoleId(Long roleId);
    List<IamRoleExclusion> findByExcludedRoleId(Long excludedRoleId);
}
