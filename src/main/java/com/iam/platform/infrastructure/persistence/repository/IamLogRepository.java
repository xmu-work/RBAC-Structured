package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IamLogRepository extends JpaRepository<IamLog, Long>, JpaSpecificationExecutor<IamLog> {
    List<IamLog> findByOperateTimeBetween(LocalDateTime start, LocalDateTime end);
    List<IamLog> findByUserId(Long userId);
    Page<IamLog> findByUserId(Long userId, Pageable pageable);
}
