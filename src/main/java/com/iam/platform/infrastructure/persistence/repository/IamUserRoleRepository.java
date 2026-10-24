package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IamUserRoleRepository extends JpaRepository<IamUserRole, Long> {
    List<IamUserRole> findByUserId(Long userId);
    List<IamUserRole> findByRoleId(Long roleId);
    List<IamUserRole> findByUserIdAndStatus(Long userId, String status);
    Optional<IamUserRole> findByUserIdAndRoleId(Long userId, Long roleId);
}
