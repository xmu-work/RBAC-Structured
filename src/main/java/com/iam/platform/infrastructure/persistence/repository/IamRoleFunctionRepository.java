package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamRoleFunction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IamRoleFunctionRepository extends JpaRepository<IamRoleFunction, Long> {
    List<IamRoleFunction> findByRoleId(Long roleId);
    List<IamRoleFunction> findByFunctionPointId(Long functionPointId);
}
