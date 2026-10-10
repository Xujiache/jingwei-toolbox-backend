package com.xujiache.jingweitoolbox.tool;

import java.time.Instant;

public record Usage(Long limit, long used, Long remaining, Instant resetAt) {
}
