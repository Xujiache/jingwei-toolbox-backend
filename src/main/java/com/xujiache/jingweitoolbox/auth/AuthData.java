package com.xujiache.jingweitoolbox.auth;

import com.xujiache.jingweitoolbox.common.security.PrincipalType;
import java.time.Instant;

public record AuthData(String accessToken, String refreshToken, String tokenType, Instant expiresAt,
        PrincipalType principalType) {
}
