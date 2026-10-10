package com.xujiache.jingweitoolbox.tool;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record ToolExecuteRequest(@Size(max = 32) String toolVersion,
        @NotNull Map<String, Object> input,
        @NotBlank @Size(max = 128) String clientRequestId) {
}
