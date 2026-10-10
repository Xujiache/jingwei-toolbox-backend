package com.xujiache.jingweitoolbox.tool;

public record IdempotencyRecord(String principalKey, String toolId, String clientRequestId, String requestHash,
        String state, String responseJson) {
    public boolean completed() {
        return "COMPLETED".equals(state);
    }
}
