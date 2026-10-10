package com.xujiache.jingweitoolbox.common.exception;

import java.util.Map;

public class ApiException extends RuntimeException {
    private final int status;
    private final String code;
    private final Map<String, Object> details;

    public ApiException(int status, String code, String message) {
        this(status, code, message, null);
    }

    public ApiException(int status, String code, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.code = code;
        this.details = details;
    }

    public int status() {
        return status;
    }

    public String code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, "BAD_REQUEST", message);
    }

    public static ApiException unauthenticated(String message) {
        return new ApiException(401, "UNAUTHENTICATED", message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(403, "FORBIDDEN", message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, "TOOL_NOT_FOUND", message);
    }

    public static ApiException conflict(String code, String message) {
        return new ApiException(409, code, message);
    }

    public static ApiException quotaExceeded(String message, Map<String, Object> details) {
        return new ApiException(429, "TOOL_QUOTA_EXCEEDED", message, details);
    }

    public static ApiException dependency(String message) {
        return new ApiException(502, "DEPENDENCY_ERROR", message);
    }

    public static ApiException unavailable(String message) {
        return new ApiException(503, "SERVICE_UNAVAILABLE", message);
    }

    public static ApiException toolNotImplemented(String toolId) {
        return new ApiException(501, "TOOL_NOT_IMPLEMENTED", "工具执行器尚未接入", Map.of("toolId", toolId));
    }
}
