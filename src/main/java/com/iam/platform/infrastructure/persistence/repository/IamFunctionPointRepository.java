package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamFunctionPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IamFunctionPointRepository extends JpaRepository<IamFunctionPoint, Long> {
    List<IamFunctionPoint> findByModuleId(Long moduleId);
    Optional<IamFunctionPoint> findByApiPathAndApiMethod(String apiPath, String apiMethod);
}
