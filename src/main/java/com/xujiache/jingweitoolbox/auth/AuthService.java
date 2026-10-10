package com.xujiache.jingweitoolbox.auth;

import com.xujiache.jingweitoolbox.common.exception.ApiException;
import com.xujiache.jingweitoolbox.common.security.AuthenticatedPrincipal;
import com.xujiache.jingweitoolbox.common.security.JwtService;
import com.xujiache.jingweitoolbox.common.security.JwtService.IssuedAccessToken;
import com.xujiache.jingweitoolbox.common.security.PrincipalType;
import com.xujiache.jingweitoolbox.common.security.RevocationService;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RevocationService revocationService;

    public AuthService(UserRepository userRepository, RefreshSessionRepository refreshSessionRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService, RevocationService revocationService) {
        this.userRepository = userRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.revocationService = revocationService;
    }

    @Transactional
    public AuthData register(RegisterRequest request) {
        String identifier = normalizeIdentifier(request.identifier());
        if (userRepository.findByIdentifier(identifier).isPresent()) {
            throw ApiException.conflict("RESOURCE_CONFLICT", "账号已存在");
        }
        String displayName = normalizeDisplayName(request.displayName(), identifier);
        try {
            UserAccount account = userRepository.insert(identifier, passwordEncoder.encode(request.password()), displayName,
                    UserRole.USER);
            return issueForUser(account);
        } catch (DataIntegrityViolationException exception) {
            throw ApiException.conflict("RESOURCE_CONFLICT", "账号已存在");
        }
    }

    @Transactional
    public AuthData login(LoginRequest request) {
        String identifier = normalizeIdentifier(request.identifier());
        UserAccount account = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> ApiException.unauthenticated("账号或密码错误"));
        if (!account.active() || !passwordEncoder.matches(request.password(), account.passwordHash())) {
            throw ApiException.unauthenticated("账号或密码错误");
        }
        return issueForUser(account);
    }

    @Transactional
    public AuthData guestSession() {
        String guestId = UUID.randomUUID().toString();
        return issue(guestId, PrincipalType.GUEST, null, Set.of("TOOL_READ", "TOOL_EXECUTE"));
    }

    @Transactional
    public AuthData refresh(RefreshRequest request) {
        String tokenHash = jwtService.hashRefreshToken(request.refreshToken());
        RefreshSession session = refreshSessionRepository.findActiveByHash(tokenHash)
                .orElseThrow(() -> ApiException.unauthenticated("刷新令牌无效或已过期"));
        refreshSessionRepository.revokeBySessionId(session.sessionId());
        if (session.principalType() == PrincipalType.GUEST) {
            return issue(session.guestId(), PrincipalType.GUEST, null, Set.of("TOOL_READ", "TOOL_EXECUTE"), session.sessionId());
        }
        UserAccount account = userRepository.findById(session.userId())
                .filter(UserAccount::active)
                .orElseThrow(() -> ApiException.unauthenticated("账号已停用"));
        return issue(account.idAsString(), principalType(account), account.displayName(), scopes(account));
    }

    public void logout(AuthenticatedPrincipal principal) {
        Duration ttl = Duration.ofSeconds(Math.max(1, principal.expiresAtEpochSeconds() - Instant.now().getEpochSecond()));
        revocationService.revoke(principal.tokenId(), ttl);
        refreshSessionRepository.revokeBySessionId(principal.sessionId());
    }

    public PrincipalData currentPrincipal(AuthenticatedPrincipal principal) {
        if (principal.isGuest()) {
            return new PrincipalData(PrincipalType.GUEST, null, null, principal.scopes());
        }
        UserAccount account = userRepository.findById(Long.parseLong(principal.subject()))
                .filter(UserAccount::active)
                .orElseThrow(() -> ApiException.unauthenticated("账号不存在或已停用"));
        return new PrincipalData(principalType(account), Long.toString(account.id()), account.displayName(), scopes(account));
    }

    private AuthData issueForUser(UserAccount account) {
        return issue(account.idAsString(), principalType(account), account.displayName(), scopes(account));
    }

    private AuthData issue(String subject, PrincipalType type, String displayName, Set<String> scopes) {
        return issue(subject, type, displayName, scopes, UUID.randomUUID().toString());
    }

    private AuthData issue(String subject, PrincipalType type, String displayName, Set<String> scopes, String sessionId) {
        String refreshToken = jwtService.newRefreshToken();
        Instant refreshExpiresAt = Instant.now().plus(jwtService.refreshTtl());
        Long userId = type == PrincipalType.GUEST ? null : Long.valueOf(subject);
        String guestId = type == PrincipalType.GUEST ? subject : null;
        refreshSessionRepository.insert(sessionId, jwtService.hashRefreshToken(refreshToken), type, userId, guestId,
                refreshExpiresAt);
        IssuedAccessToken accessToken = jwtService.issueAccessToken(subject, type, displayName, scopes, sessionId);
        return new AuthData(accessToken.token(), refreshToken, "Bearer", accessToken.expiresAt(), type);
    }

    private static String normalizeIdentifier(String identifier) {
        return identifier.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static String normalizeDisplayName(String displayName, String identifier) {
        if (displayName == null || displayName.isBlank()) {
            return identifier;
        }
        return displayName.trim();
    }

    private static PrincipalType principalType(UserAccount account) {
        return account.role() == UserRole.ADMIN ? PrincipalType.ADMIN : PrincipalType.USER;
    }

    private static Set<String> scopes(UserAccount account) {
        return account.role() == UserRole.ADMIN
                ? Set.of("TOOL_READ", "TOOL_EXECUTE", "ADMIN_TOOLS", "ADMIN_USAGE")
                : Set.of("TOOL_READ", "TOOL_EXECUTE");
    }
}
