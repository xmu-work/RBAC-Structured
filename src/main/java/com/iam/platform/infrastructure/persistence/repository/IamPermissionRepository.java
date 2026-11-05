package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IamPermissionRepository extends JpaRepository<IamPermission, Long> {
    Optional<IamPermission> findBySystemIdAndPermissionCode(Long systemId, String permissionCode);
    List<IamPermission> findBySystemIdAndIsDeleted(Long systemId, Integer isDeleted);
}
