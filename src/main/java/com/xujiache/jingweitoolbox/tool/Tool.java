package com.xujiache.jingweitoolbox.tool;

import java.time.Instant;
import java.util.Map;

public record Tool(String toolId, String displayName, String description, ToolStatus status, String currentVersion,
        Map<String, Object> inputSchema, Map<String, Object> outputSchema, ToolPolicy policy, long version,
        Instant updatedAt) {
    public boolean publiclyAvailable() {
        return policy.enabled() && status == ToolStatus.PUBLISHED;
    }
}
