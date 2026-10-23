package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IamTokenRepository extends JpaRepository<IamToken, String> {
    Optional<IamToken> findByTokenHash(String tokenHash);
    List<IamToken> findByUserId(Long userId);
    List<IamToken> findByUserIdAndStatus(Long userId, String status);
}
