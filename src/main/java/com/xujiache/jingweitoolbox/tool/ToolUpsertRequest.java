package com.xujiache.jingweitoolbox.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record ToolUpsertRequest(
        @NotBlank @Size(max = 64) String toolId,
        @NotBlank @Size(max = 128) String displayName,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 32) String currentVersion,
        @NotNull ToolStatus status,
        Map<String, Object> inputSchema,
        Map<String, Object> outputSchema) {
}
