package com.xujiache.jingweitoolbox.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Stable response envelope shared by the HarmonyOS client and admin console. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String code, String message, T data, String requestId) {
    public static <T> ApiResponse<T> ok(T data, String requestId) {
        return new ApiResponse<>("OK", "OK", data, requestId);
    }

    public static <T> ApiResponse<T> accepted(T data, String requestId) {
        return new ApiResponse<>("ACCEPTED", "Accepted", data, requestId);
    }
}
