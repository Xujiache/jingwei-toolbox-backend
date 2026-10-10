package com.xujiache.jingweitoolbox.tool;

import jakarta.validation.constraints.Min;

public record ToolPolicyPatch(Boolean enabled, Boolean loginRequired, @Min(0) Long guestLimit,
        @Min(1) Long guestLimitWindowSeconds, Boolean clearGuestQuota, @Min(0) long expectedVersion) {
    public boolean hasChanges() {
        return enabled != null || loginRequired != null || guestLimit != null || guestLimitWindowSeconds != null
                || Boolean.TRUE.equals(clearGuestQuota);
    }
}
