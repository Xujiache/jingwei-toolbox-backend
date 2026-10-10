package com.xujiache.jingweitoolbox.tool;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record ToolPatchRequest(
        @Size(max = 128) String displayName,
        @Size(max = 2000) String description,
        @Size(max = 32) String currentVersion,
        ToolStatus status,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema,
        @Min(0) long expectedVersion) {
    public boolean hasChanges() {
        return displayName != null || description != null || currentVersion != null || status != null
                || inputSchema != null || outputSchema != null;
    }
}
