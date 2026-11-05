package com.iam.platform.interfaces.admin;

import com.iam.platform.application.system.SystemService;
import com.iam.platform.common.result.Result;
import com.iam.platform.infrastructure.persistence.entity.IamSystem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 多系统资源控制器。只暴露 {@code /systems} 资源。
 */
@Validated
@RestController
@RequestMapping("/systems")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @PostMapping
    public Result<IamSystem> register(@RequestParam @NotBlank @Size(max = 64) String systemCode,
                                      @RequestParam @NotBlank @Size(max = 128) String systemName,
                                      @RequestParam(required = false) @Size(max = 500) String description) {
        return Result.success(systemService.registerSystem(systemCode, systemName, description));
    }

    @GetMapping
    public Result<List<IamSystem>> list() {
        return Result.success(systemService.listSystems());
    }

    @GetMapping("/{systemId}")
    public Result<IamSystem> get(@PathVariable Long systemId) {
        return Result.success(systemService.getSystem(systemId));
    }
}
