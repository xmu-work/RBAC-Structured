package com.iam.platform.infrastructure.persistence.repository;

import com.iam.platform.infrastructure.persistence.entity.IamAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IamAuditLogRepository extends JpaRepository<IamAuditLog, Long>, JpaSpecificationExecutor<IamAuditLog> {
    List<IamAuditLog> findByOperateTimeBetween(LocalDateTime start, LocalDateTime end);
    List<IamAuditLog> findByUserId(Long userId);
    Page<IamAuditLog> findByUserId(Long userId, Pageable pageable);
}