package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface IamRolePermissionRepository extends JpaRepository<IamRolePermission, Long> {

    List<IamRolePermission> findByRoleId(Long roleId);

    Optional<IamRolePermission> findByRoleIdAndPermissionId(Long roleId, Long permissionId);

    /**
     * 一次查询取得“角色 → 前端权限”候选，并按系统、权限和角色状态过滤。
     */
    @Query("SELECT rp.permissionId AS permissionId, rp.allowedAction AS allowedAction, "
            + "p.action AS requiredAction "
            + "FROM IamRolePermission rp, IamPermission p, IamSystem s "
            + "WHERE rp.roleId IN :roleIds "
            + "AND p.id = rp.permissionId AND p.isDeleted = 0 AND p.status = 'ACTIVE' "
            + "AND s.id = p.systemId AND s.systemCode = :systemCode "
            + "AND s.isDeleted = 0 AND s.status = 'ACTIVE'")
    List<GrantedPermissionView> findGrantedPermissions(@Param("roleIds") Collection<Long> roleIds,
                                                       @Param("systemCode") String systemCode);

    interface GrantedPermissionView {
        Long getPermissionId();
        String getAllowedAction();
        String getRequiredAction();
    }
}
