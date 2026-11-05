package com.iam.platform.application.system;

import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamSystem;
import com.iam.platform.infrastructure.persistence.repository.IamSystemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 多系统登记能力。只管理接入系统本身，不承载权限或 API。
 */
@Service
@RequiredArgsConstructor
public class SystemService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final IamSystemRepository systemRepository;

    @Transactional
    public IamSystem registerSystem(String systemCode, String systemName, String description) {
        if (systemRepository.findBySystemCode(systemCode).isPresent()) {
            throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE, "系统编码已存在: " + systemCode);
        }
        LocalDateTime now = LocalDateTime.now();
        IamSystem system = new IamSystem();
        system.setSystemCode(systemCode);
        system.setSystemName(systemName);
        system.setDescription(description);
        system.setStatus(STATUS_ACTIVE);
        system.setIsDeleted(0);
        system.setCreatedAt(now);
        system.setUpdatedAt(now);
        return systemRepository.save(system);
    }

    public List<IamSystem> listSystems() {
        return systemRepository.findAll().stream().filter(s -> s.getIsDeleted() == 0).toList();
    }

    public IamSystem getSystem(Long systemId) {
        return systemRepository.findById(systemId)
                .filter(s -> s.getIsDeleted() == 0)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "系统不存在: " + systemId));
    }
}
