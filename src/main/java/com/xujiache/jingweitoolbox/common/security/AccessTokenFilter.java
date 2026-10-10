package com.xujiache.jingweitoolbox.common.security;

import com.xujiache.jingweitoolbox.common.exception.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.http.MediaType;
import java.nio.charset.StandardCharsets;
import com.xujiache.jingweitoolbox.common.web.RequestContext;

public class AccessTokenFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final RevocationService revocationService;

    public AccessTokenFilter(JwtService jwtService, RevocationService revocationService) {
        this.jwtService = jwtService;
        this.revocationService = revocationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String header = request.getHeader("Authorization");
            if (header != null && !header.isBlank()) {
                if (!header.startsWith("Bearer ")) {
                    throw ApiException.unauthenticated("Authorization 必须使用 Bearer 令牌");
                }
                String token = header.substring(7).trim();
                if (token.isEmpty()) {
                    throw ApiException.unauthenticated("访问令牌不能为空");
                }
                AuthenticatedPrincipal principal = jwtService.parseAccessToken(token);
                if (revocationService.isRevoked(principal.tokenId())) {
                    throw ApiException.unauthenticated("会话已注销");
                }
                var authorities = principal.scopes().stream()
                        .map(scope -> new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_" + scope))
                        .collect(java.util.stream.Collectors.toSet());
                if (principal.isAdmin()) {
                    authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"));
                }
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(principal, token, authorities));
            }
            filterChain.doFilter(request, response);
        } catch (ApiException exception) {
            writeError(response, exception.status(), exception.code(), exception.getMessage());
        } catch (IllegalStateException exception) {
            writeError(response, 503, "SERVICE_UNAVAILABLE", "认证依赖暂不可用");
        }
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message
                + "\",\"requestId\":\"" + RequestContext.requestId() + "\"}");
    }
}
