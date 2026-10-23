package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamDataScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IamDataScopeRepository extends JpaRepository<IamDataScope, Long> {
    Optional<IamDataScope> findByScopeCode(String scopeCode);
}
