package com.xujiache.jingweitoolbox.admin;

import com.xujiache.jingweitoolbox.common.api.ApiResponse;
import com.xujiache.jingweitoolbox.common.api.PageResponse;
import com.xujiache.jingweitoolbox.common.security.AuthenticatedPrincipal;
import com.xujiache.jingweitoolbox.common.web.RequestContext;
import com.xujiache.jingweitoolbox.tool.Tool;
import com.xujiache.jingweitoolbox.tool.ToolPatchRequest;
import com.xujiache.jingweitoolbox.tool.ToolPolicyPatch;
import com.xujiache.jingweitoolbox.tool.ToolService;
import com.xujiache.jingweitoolbox.tool.ToolStatus;
import com.xujiache.jingweitoolbox.tool.ToolUpsertRequest;
import com.xujiache.jingweitoolbox.tool.UsageSummary;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/tools")
@PreAuthorize("hasRole('ADMIN')")
public class AdminToolController {
    private final ToolService toolService;

    public AdminToolController(ToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping
    public ApiResponse<PageResponse<Tool>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize, @RequestParam Optional<ToolStatus> status) {
        return ApiResponse.ok(toolService.listAdmin(status.orElse(null), page, pageSize), RequestContext.requestId());
    }

    @PostMapping
    public ApiResponse<Tool> create(@Valid @RequestBody ToolUpsertRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.ok(toolService.create(request, principal), RequestContext.requestId());
    }

    @PatchMapping("/{toolId}")
    public ApiResponse<Tool> update(@PathVariable String toolId, @Valid @RequestBody ToolPatchRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.ok(toolService.updateMetadata(toolId, request, principal), RequestContext.requestId());
    }

    @PatchMapping("/{toolId}/policy")
    public ApiResponse<Tool> policy(@PathVariable String toolId, @Valid @RequestBody ToolPolicyPatch request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.ok(toolService.updatePolicy(toolId, request, principal), RequestContext.requestId());
    }

    @GetMapping("/{toolId}/usage")
    public ApiResponse<UsageSummary> usage(@PathVariable String toolId,
            @RequestParam(required = false) Instant from, @RequestParam(required = false) Instant to) {
        return ApiResponse.ok(toolService.usage(toolId, from, to), RequestContext.requestId());
    }
}
