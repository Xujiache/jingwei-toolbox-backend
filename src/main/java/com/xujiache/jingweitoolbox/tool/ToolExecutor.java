package com.xujiache.jingweitoolbox.tool;

import com.xujiache.jingweitoolbox.common.security.AuthenticatedPrincipal;
import java.util.Map;

/** Concrete tools register an executor here; the platform deliberately does not fake tool results. */
public interface ToolExecutor {
    String toolId();

    Map<String, Object> execute(ToolExecutionContext context) throws Exception;

    record ToolExecutionContext(Tool tool, String toolVersion, Map<String, Object> input,
            AuthenticatedPrincipal principal, String requestId) {
    }
}
