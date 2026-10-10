package com.xujiache.jingweitoolbox.tool;

public record ToolPolicy(boolean enabled, boolean loginRequired, Long guestLimit, Long guestLimitWindowSeconds) {
    public boolean hasGuestQuota() {
        return guestLimit != null;
    }
}
