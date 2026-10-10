package com.xujiache.jingweitoolbox.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private final JwtService jwt = new JwtService(JsonMapper.builder().build(),
            "01234567890123456789012345678901", "15m", "30d");

    @Test
    void roundTripPreservesPrincipalAndSession() {
        JwtService.IssuedAccessToken issued = jwt.issueAccessToken("guest-1", PrincipalType.GUEST, null,
                Set.of("TOOL_EXECUTE"), "session-1");

        AuthenticatedPrincipal parsed = jwt.parseAccessToken(issued.token());

        assertEquals("guest-1", parsed.subject());
        assertEquals(PrincipalType.GUEST, parsed.principalType());
        assertEquals("session-1", parsed.sessionId());
        assertEquals(Set.of("TOOL_EXECUTE"), parsed.scopes());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService.IssuedAccessToken issued = jwt.issueAccessToken("user-1", PrincipalType.USER, "user",
                Set.of(), "session-2");
        String tampered = issued.token().substring(0, issued.token().length() - 1) + "x";

        assertThrows(RuntimeException.class, () -> jwt.parseAccessToken(tampered));
    }

    @Test
    void shortSecretFailsClosed() {
        JwtService insecure = new JwtService(JsonMapper.builder().build(), "short", "15m", "30d");

        assertThrows(RuntimeException.class,
                () -> insecure.issueAccessToken("user-1", PrincipalType.USER, null, Set.of(), "session-3"));
    }
}
