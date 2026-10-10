package com.xujiache.jingweitoolbox.auth;

import com.xujiache.jingweitoolbox.common.api.ApiResponse;
import com.xujiache.jingweitoolbox.common.security.AuthenticatedPrincipal;
import com.xujiache.jingweitoolbox.common.web.RequestContext;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {
    private final AuthService authService;

    public MeController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<PrincipalData> me(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.ok(authService.currentPrincipal(principal), RequestContext.requestId());
    }
}
