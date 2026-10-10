package com.xujiache.jingweitoolbox.common.security;

import java.time.Duration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RevocationService {
    private static final String KEY_PREFIX = "jingwei:auth:revoked:";
    private final StringRedisTemplate redis;

    public RevocationService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void revoke(String tokenId, Duration ttl) {
        if (tokenId == null || tokenId.isBlank()) {
            return;
        }
        try {
            redis.opsForValue().set(KEY_PREFIX + tokenId, "1", ttl);
        } catch (DataAccessException exception) {
            throw new IllegalStateException("Redis 不可用，无法撤销会话", exception);
        }
    }

    public boolean isRevoked(String tokenId) {
        try {
            return Boolean.TRUE.equals(redis.hasKey(KEY_PREFIX + tokenId));
        } catch (DataAccessException exception) {
            throw new IllegalStateException("Redis 不可用，无法校验会话", exception);
        }
    }
}
