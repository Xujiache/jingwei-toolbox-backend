package com.xujiache.jingweitoolbox.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xujiache.jingweitoolbox.common.exception.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private static final String ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtService(ObjectMapper objectMapper,
            @Value("${app.security.jwt-secret:}") String secret,
            @Value("${app.security.access-token-ttl:15m}") String accessTtl,
            @Value("${app.security.refresh-token-ttl:30d}") String refreshTtl) {
        this.objectMapper = objectMapper;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTtl = parseDuration(accessTtl);
        this.refreshTtl = parseDuration(refreshTtl);
    }

    public IssuedAccessToken issueAccessToken(String subject, PrincipalType principalType, String displayName,
            Set<String> scopes, String sessionId) {
        ensureSecret();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(accessTtl);
        String tokenId = randomToken(18);
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", subject);
        claims.put("principalType", principalType.name());
        claims.put("displayName", displayName);
        claims.put("scopes", scopes == null ? Set.of() : scopes);
        claims.put("iat", now.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        claims.put("jti", tokenId);
        claims.put("sid", sessionId);
        return new IssuedAccessToken(sign(claims), expiresAt, tokenId);
    }

    public String newRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    public String hashRefreshToken(String token) {
        try {
            return URL_ENCODER.encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算令牌摘要", exception);
        }
    }

    public Duration refreshTtl() {
        return refreshTtl;
    }

    public AuthenticatedPrincipal parseAccessToken(String token) {
        try {
            ensureSecret();
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) {
                throw invalidToken();
            }
            Map<String, Object> header = objectMapper.readValue(URL_DECODER.decode(parts[0]), new TypeReference<>() {
            });
            if (!"HS256".equals(header.get("alg"))) {
                throw invalidToken();
            }
            String signed = parts[0] + "." + parts[1];
            byte[] expected = signBytes(signed.getBytes(StandardCharsets.US_ASCII));
            byte[] supplied = URL_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, supplied)) {
                throw invalidToken();
            }
            Map<String, Object> claims = objectMapper.readValue(URL_DECODER.decode(parts[1]), new TypeReference<>() {
            });
            long exp = longClaim(claims, "exp");
            if (exp <= Instant.now().getEpochSecond()) {
                throw invalidToken();
            }
            String subject = stringClaim(claims, "sub");
            PrincipalType principalType = PrincipalType.valueOf(stringClaim(claims, "principalType"));
            String displayName = claims.get("displayName") == null ? null : String.valueOf(claims.get("displayName"));
            String tokenId = stringClaim(claims, "jti");
            String sessionId = stringClaim(claims, "sid");
            Set<String> scopes = new HashSet<>();
            Object scopeClaim = claims.get("scopes");
            if (scopeClaim instanceof Iterable<?> iterable) {
                for (Object value : iterable) {
                    scopes.add(String.valueOf(value));
                }
            }
            return new AuthenticatedPrincipal(subject, principalType, displayName, Set.copyOf(scopes), tokenId, sessionId, exp);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalidToken();
        }
    }

    private String sign(Map<String, Object> claims) {
        try {
            String header = URL_ENCODER.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
            String payload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
            String signed = header + "." + payload;
            return signed + "." + URL_ENCODER.encodeToString(signBytes(signed.getBytes(StandardCharsets.US_ASCII)));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法生成访问令牌", exception);
        }
    }

    private byte[] signBytes(byte[] value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            return mac.doFinal(value);
        } catch (Exception exception) {
            throw new IllegalStateException("无法签名访问令牌", exception);
        }
    }

    private void ensureSecret() {
        if (secret.length < 32) {
            throw ApiException.unavailable("JWT_SECRET 未配置或长度不足 32 字节");
        }
    }

    private static String stringClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (!(value instanceof String string) || string.isBlank()) {
            throw invalidToken();
        }
        return string;
    }

    private static long longClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw invalidToken();
    }

    private static ApiException invalidToken() {
        return ApiException.unauthenticated("访问令牌无效或已过期");
    }

    private static Duration parseDuration(String raw) {
        String value = raw.trim().toLowerCase();
        if (value.matches("\\d+")) {
            return Duration.ofSeconds(Long.parseLong(value));
        }
        long multiplier;
        String number;
        if (value.endsWith("ms")) {
            multiplier = 1;
            number = value.substring(0, value.length() - 2);
            return Duration.ofMillis(Long.parseLong(number));
        } else if (value.endsWith("s")) {
            multiplier = 1;
            number = value.substring(0, value.length() - 1);
        } else if (value.endsWith("m")) {
            multiplier = 60;
            number = value.substring(0, value.length() - 1);
        } else if (value.endsWith("h")) {
            multiplier = 3600;
            number = value.substring(0, value.length() - 1);
        } else if (value.endsWith("d")) {
            multiplier = 86400;
            number = value.substring(0, value.length() - 1);
        } else {
            throw new IllegalArgumentException("不支持的令牌有效期: " + raw);
        }
        return Duration.ofSeconds(Long.parseLong(number) * multiplier);
    }

    private String randomToken(int bytes) {
        byte[] value = new byte[bytes];
        secureRandom.nextBytes(value);
        return URL_ENCODER.encodeToString(value);
    }

    public record IssuedAccessToken(String token, Instant expiresAt, String tokenId) {
    }
}
