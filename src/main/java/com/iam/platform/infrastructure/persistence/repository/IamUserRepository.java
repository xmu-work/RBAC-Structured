package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IamUserRepository extends JpaRepository<IamUser, Long> {
    Optional<IamUser> findByUserNo(String userNo);
    List<IamUser> findByOrgId(Long orgId);
    List<IamUser> findByAccountStatus(String accountStatus);
    Optional<IamUser> findByPhoneHash(String phoneHash);
}
