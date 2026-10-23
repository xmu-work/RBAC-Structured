package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamRoleDataScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IamRoleDataScopeRepository extends JpaRepository<IamRoleDataScope, Long> {
    List<IamRoleDataScope> findByRoleId(Long roleId);
    List<IamRoleDataScope> findByScopeType(String scopeType);
}
