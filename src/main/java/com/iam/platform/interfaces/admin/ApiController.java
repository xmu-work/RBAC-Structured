package com.iam.platform.interfaces.admin;

import com.iam.platform.application.api.ApiService;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamApi;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 后端 API 资源控制器。
 */
@Validated
@RestController
@RequestMapping("/apis")
@RequiredArgsConstructor
public class ApiController {

    private final ApiService apiService;

    @PostMapping
    public Result<IamApi> register(
            @RequestParam @NotNull Long systemId,
            @RequestParam @NotBlank @Size(max = 64) String apiCode,
            @RequestParam @NotBlank @Size(max = 128) String apiName,
            @RequestParam @NotBlank @Size(max = 255) String apiPath,
            @RequestParam @NotBlank @Size(max = 16) String httpMethod) {
        return Result.success(apiService.registerApi(systemId, apiCode, apiName, apiPath, httpMethod));
    }

    @GetMapping
    public Result<List<IamApi>> list(@RequestParam @NotNull Long systemId) {
        return Result.success(apiService.listApis(systemId));
    }

    @GetMapping("/{apiId}")
    public Result<IamApi> get(@PathVariable Long apiId) {
        return Result.success(apiService.getApi(apiId));
    }
}
