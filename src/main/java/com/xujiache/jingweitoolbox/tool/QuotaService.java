package com.xujiache.jingweitoolbox.tool;

import com.xujiache.jingweitoolbox.common.exception.ApiException;
import com.xujiache.jingweitoolbox.common.security.AuthenticatedPrincipal;
import java.time.Instant;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class QuotaService {
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local limit = tonumber(ARGV[1])
            if current >= limit then return -1 end
            current = redis.call('INCR', KEYS[1])
            if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[2]) end
            return current
            """, Long.class);

    private final StringRedisTemplate redis;

    public QuotaService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public Usage consume(Tool tool, AuthenticatedPrincipal principal) {
        ToolPolicy policy = tool.policy();
        if (!principal.isGuest() || policy.guestLimit() == null) {
            return new Usage(null, 0, null, null);
        }
        long limit = policy.guestLimit();
        long window = policy.guestLimitWindowSeconds() == null ? 86_400L : policy.guestLimitWindowSeconds();
        long now = Instant.now().getEpochSecond();
        long windowStart = (now / window) * window;
        Instant resetAt = Instant.ofEpochSecond(windowStart + window);
        String key = key(tool.toolId(), principal.subject(), windowStart);
        try {
            Long count = redis.execute(CONSUME_SCRIPT, java.util.List.of(key), Long.toString(limit), Long.toString(window));
            if (count == null) {
                throw ApiException.unavailable("Redis 未返回额度结果");
            }
            if (count < 0 || count > limit) {
                throw ApiException.quotaExceeded("工具游客额度已用尽", Map.of("limit", limit, "used", Math.max(limit, count),
                        "remaining", 0, "resetAt", resetAt.toString()));
            }
            return new Usage(limit, count, limit - count, resetAt);
        } catch (ApiException exception) {
            throw exception;
        } catch (DataAccessException exception) {
            throw ApiException.unavailable("Redis 暂不可用，无法校验工具额度");
        }
    }

    public Usage current(Tool tool, AuthenticatedPrincipal principal) {
        ToolPolicy policy = tool.policy();
        if (!principal.isGuest() || policy.guestLimit() == null) {
            return new Usage(null, 0, null, null);
        }
        long window = policy.guestLimitWindowSeconds() == null ? 86_400L : policy.guestLimitWindowSeconds();
        long now = Instant.now().getEpochSecond();
        long windowStart = (now / window) * window;
        Instant resetAt = Instant.ofEpochSecond(windowStart + window);
        try {
            String value = redis.opsForValue().get(key(tool.toolId(), principal.subject(), windowStart));
            long used = value == null ? 0 : Long.parseLong(value);
            return new Usage(policy.guestLimit(), used, Math.max(0, policy.guestLimit() - used), resetAt);
        } catch (DataAccessException | NumberFormatException exception) {
            throw ApiException.unavailable("Redis 暂不可用，无法读取工具额度");
        }
    }

    private String key(String toolId, String principalId, long windowStart) {
        return "jingwei:quota:" + toolId + ":" + principalId + ":" + windowStart;
    }
}
