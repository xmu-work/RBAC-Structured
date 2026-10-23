package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamPermissionChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IamPermissionChangeLogRepository extends JpaRepository<IamPermissionChangeLog, Long> {
    List<IamPermissionChangeLog> findByUserId(Long userId);
    List<IamPermissionChangeLog> findByRoleId(Long roleId);
}
