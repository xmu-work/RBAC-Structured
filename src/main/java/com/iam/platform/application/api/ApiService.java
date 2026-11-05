package com.iam.platform.application.api;

import com.iam.platform.application.system.SystemService;
import com.iam.platform.common.enums.ErrorCode;
import com.iam.platform.common.exception.BusinessException;
import com.iam.platform.infrastructure.persistence.entity.IamApi;
import com.iam.platform.infrastructure.persistence.repository.IamApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 后端 API 目录能力。只负责 API 元数据和请求匹配，不承载角色授权。
 */
@Service
@RequiredArgsConstructor
public class ApiService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final IamApiRepository apiRepository;
    private final SystemService systemService;

    @Transactional
    public IamApi registerApi(Long systemId, String apiCode, String apiName,
                              String apiPath, String httpMethod) {
        systemService.getSystem(systemId);
        String method = httpMethod == null ? null : httpMethod.toUpperCase();
        if (method == null || method.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_MISSING, "HTTP 方法不能为空");
        }
        if (apiRepository.findBySystemIdAndApiCode(systemId, apiCode).isPresent()) {
            throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE, "API 编码已存在: " + apiCode);
        }
        if (apiRepository.findByApiPathAndHttpMethod(apiPath, method).isPresent()) {
            throw new BusinessException(ErrorCode.RESOURCE_DUPLICATE,
                    "API 路径与方法已存在: " + method + " " + apiPath);
        }

        LocalDateTime now = LocalDateTime.now();
        IamApi api = new IamApi();
        api.setSystemId(systemId);
        api.setApiCode(apiCode);
        api.setApiName(apiName);
        api.setApiPath(apiPath);
        api.setHttpMethod(method);
        api.setStatus(STATUS_ACTIVE);
        api.setIsDeleted(0);
        api.setCreatedAt(now);
        api.setUpdatedAt(now);
        return apiRepository.save(api);
    }

    public List<IamApi> listApis(Long systemId) {
        return apiRepository.findBySystemIdAndIsDeleted(systemId, 0);
    }

    public Optional<IamApi> findByRequest(String apiPath, String httpMethod) {
        return apiRepository.findByApiPathAndHttpMethod(apiPath, httpMethod.toUpperCase())
                .filter(api -> api.getIsDeleted() == 0 && STATUS_ACTIVE.equals(api.getStatus()));
    }

    public IamApi getApi(Long apiId) {
        return apiRepository.findById(apiId)
                .filter(api -> api.getIsDeleted() == 0)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND, "API 不存在: " + apiId));
    }
}
