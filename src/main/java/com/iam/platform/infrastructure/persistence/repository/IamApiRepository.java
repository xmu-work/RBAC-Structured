package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamApi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IamApiRepository extends JpaRepository<IamApi, Long> {
    Optional<IamApi> findByApiPathAndHttpMethod(String apiPath, String httpMethod);
    Optional<IamApi> findBySystemIdAndApiCode(Long systemId, String apiCode);
    List<IamApi> findBySystemIdAndIsDeleted(Long systemId, Integer isDeleted);
    List<IamApi> findByPermissionId(Long permissionId);
}
