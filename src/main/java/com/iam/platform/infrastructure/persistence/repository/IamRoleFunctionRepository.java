package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamRoleFunction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface IamRoleFunctionRepository extends JpaRepository<IamRoleFunction, Long> {
    List<IamRoleFunction> findByRoleId(Long roleId);
    List<IamRoleFunction> findByFunctionPointId(Long functionPointId);
    List<IamRoleFunction> findByRoleIdIn(Collection<Long> roleIds);

    /**
     * 一条 join 查询取“角色 → 功能点”候选，并已按子系统/模块/功能点的启用状态过滤，
     * 供决策服务用单次查询替代“查角色-功能 → 查系统 → 查模块 → 查功能点”多轮查询。
     */
    @Query("SELECT rf.functionPointId AS functionPointId, rf.allowedAction AS allowedAction, fp.action AS requiredAction "
            + "FROM IamRoleFunction rf, IamFunctionPoint fp, IamModule m, IamSystem s "
            + "WHERE rf.roleId IN :roleIds "
            + "AND fp.id = rf.functionPointId AND fp.isDeleted = 0 AND fp.status = 'ACTIVE' "
            + "AND m.id = fp.moduleId AND m.isDeleted = 0 AND m.status = 'ACTIVE' "
            + "AND s.id = m.systemId AND s.systemCode = :systemCode AND s.isDeleted = 0 AND s.status = 'ACTIVE'")
    List<GrantedPointView> findGrantedPoints(@Param("roleIds") Collection<Long> roleIds,
                                             @Param("systemCode") String systemCode);

    /** 候选功能点投影：功能点 ID、角色授予的操作、功能点声明的操作。 */
    interface GrantedPointView {
        Long getFunctionPointId();
        String getAllowedAction();
        String getRequiredAction();
    }
}