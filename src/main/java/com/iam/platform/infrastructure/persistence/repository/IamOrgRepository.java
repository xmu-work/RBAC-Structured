package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamOrg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IamOrgRepository extends JpaRepository<IamOrg, Long> {
    List<IamOrg> findByParentId(Long parentId);
    List<IamOrg> findByOrgType(String orgType);
}
