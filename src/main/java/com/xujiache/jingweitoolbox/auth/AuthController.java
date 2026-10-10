package com.xujiache.jingweitoolbox.auth;

import com.xujiache.jingweitoolbox.common.api.ApiResponse;
import com.xujiache.jingweitoolbox.common.security.AuthenticatedPrincipal;
import com.xujiache.jingweitoolbox.common.web.RequestContext;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/guest-session")
    public ApiResponse<AuthData> guestSession() {
        return ApiResponse.ok(authService.guestSession(), RequestContext.requestId());
    }

    @PostMapping("/register")
    public ApiResponse<AuthData> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request), RequestContext.requestId());
    }

    @PostMapping("/login")
    public ApiResponse<AuthData> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request), RequestContext.requestId());
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthData> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok(authService.refresh(request), RequestContext.requestId());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        authService.logout(principal);
        return ResponseEntity.noContent().build();
    }
}
