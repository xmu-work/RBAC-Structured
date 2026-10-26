package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamOrg;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IamOrgRepository extends JpaRepository<IamOrg, Long> {
    List<IamOrg> findByParentId(Long parentId);
    List<IamOrg> findByOrgType(String orgType);
    List<IamOrg> findByParentIdIsNull();
    List<IamOrg> findByAncestorsStartingWith(String prefix);
    Optional<IamOrg> findByOrgCode(String orgCode);
}
