package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IamModuleRepository extends JpaRepository<IamModule, Long> {
    List<IamModule> findBySystemId(Long systemId);
}
