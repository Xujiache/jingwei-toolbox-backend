package com.xujiache.jingweitoolbox.tool;

import com.xujiache.jingweitoolbox.common.api.ApiResponse;
import com.xujiache.jingweitoolbox.common.api.PageResponse;
import com.xujiache.jingweitoolbox.common.security.AuthenticatedPrincipal;
import com.xujiache.jingweitoolbox.common.web.RequestContext;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tools")
public class ToolsController {
    private final ToolService toolService;

    public ToolsController(ToolService toolService) {
        this.toolService = toolService;
    }

    @GetMapping
    public ApiResponse<PageResponse<Tool>> list(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize, @RequestParam Optional<ToolStatus> status) {
        return ApiResponse.ok(toolService.listPublic(status.orElse(null), page, pageSize), RequestContext.requestId());
    }

    @GetMapping("/{toolId}")
    public ApiResponse<Tool> detail(@PathVariable String toolId) {
        return ApiResponse.ok(toolService.publicDetail(toolId), RequestContext.requestId());
    }

    @PostMapping("/{toolId}/execute")
    public ApiResponse<ToolExecuteData> execute(@PathVariable String toolId, @Valid @RequestBody ToolExecuteRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return ApiResponse.ok(toolService.execute(toolId, request, principal), RequestContext.requestId());
    }
}
