package com.xujiache.jingweitoolbox.tool;

import java.util.Map;

public record ToolExecuteData(String toolId, String toolVersion, Map<String, Object> result, Usage usage) {
}
