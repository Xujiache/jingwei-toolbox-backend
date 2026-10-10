package com.xujiache.jingweitoolbox.tool;

import java.time.Instant;

public record UsageSummary(String toolId, Instant from, Instant to, long totalExecutions, long guestExecutions,
        long userExecutions) {
}
