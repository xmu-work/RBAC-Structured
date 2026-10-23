package com.iam.platform.application.decision;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 批量权限判定结果。
 */
@Data
public class PermissionCheckResult {
    private Map<Long, Boolean> results = new HashMap<>();
}
