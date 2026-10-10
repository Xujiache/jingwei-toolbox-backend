package com.xujiache.jingweitoolbox.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xujiache.jingweitoolbox.common.api.PageResponse;
import com.xujiache.jingweitoolbox.common.exception.ApiException;
import com.xujiache.jingweitoolbox.common.security.AuthenticatedPrincipal;
import com.xujiache.jingweitoolbox.common.web.RequestContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ToolService {
    private final ToolRepository toolRepository;
    private final ToolExecutorRegistry executorRegistry;
    private final QuotaService quotaService;
    private final IdempotencyRepository idempotencyRepository;
    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    public ToolService(ToolRepository toolRepository, ToolExecutorRegistry executorRegistry, QuotaService quotaService,
            IdempotencyRepository idempotencyRepository, AuditRepository auditRepository, ObjectMapper objectMapper) {
        this.toolRepository = toolRepository;
        this.executorRegistry = executorRegistry;
        this.quotaService = quotaService;
        this.idempotencyRepository = idempotencyRepository;
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    public PageResponse<Tool> listPublic(ToolStatus status, int page, int pageSize) {
        if (status != null && status != ToolStatus.PUBLISHED) {
            throw ApiException.badRequest("客户端只能读取 PUBLISHED 工具");
        }
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizePageSize(pageSize);
        ToolStatus publicStatus = status == null ? ToolStatus.PUBLISHED : status;
        return new PageResponse<>(toolRepository.listPublic(publicStatus, normalizedPage, normalizedSize), normalizedPage,
                normalizedSize, toolRepository.countPublic(publicStatus));
    }

    public PageResponse<Tool> listAdmin(ToolStatus status, int page, int pageSize) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizePageSize(pageSize);
        return new PageResponse<>(toolRepository.listAll(status, normalizedPage, normalizedSize), normalizedPage, normalizedSize,
                toolRepository.countAll(status));
    }

    public Tool publicDetail(String toolId) {
        Tool tool = findTool(toolId);
        if (!tool.publiclyAvailable()) {
            throw ApiException.notFound("工具不存在");
        }
        return tool;
    }

    public Tool adminDetail(String toolId) {
        return findTool(toolId);
    }

    @Transactional
    public Tool create(ToolUpsertRequest request, AuthenticatedPrincipal actor) {
        Tool tool = toolRepository.insert(request);
        audit(actor, "TOOL_CREATED", request.toolId(), Map.of("toolId", request.toolId(), "status", request.status().name()));
        return tool;
    }

    @Transactional
    public Tool updateMetadata(String toolId, ToolPatchRequest request, AuthenticatedPrincipal actor) {
        if (!request.hasChanges()) {
            throw ApiException.badRequest("至少提供一个工具字段");
        }
        Tool old = findTool(toolId);
        Optional<Tool> updated = toolRepository.updateMetadata(toolId, request);
        if (updated.isEmpty()) {
            throw ApiException.conflict("RESOURCE_VERSION_CONFLICT", "工具版本已变化，请重新读取后再修改");
        }
        audit(actor, "TOOL_UPDATED", toolId, Map.of("expectedVersion", request.expectedVersion(), "previousVersion", old.version()));
        return updated.get();
    }

    @Transactional
    public Tool updatePolicy(String toolId, ToolPolicyPatch request, AuthenticatedPrincipal actor) {
        if (!request.hasChanges()) {
            throw ApiException.badRequest("至少提供一个策略字段");
        }
        Tool old = findTool(toolId);
        Long guestLimit = Boolean.TRUE.equals(request.clearGuestQuota()) ? null
                : (request.guestLimit() == null ? old.policy().guestLimit() : request.guestLimit());
        Long window = Boolean.TRUE.equals(request.clearGuestQuota()) ? null
                : (request.guestLimitWindowSeconds() == null ? old.policy().guestLimitWindowSeconds()
                        : request.guestLimitWindowSeconds());
        if (guestLimit == null) {
            window = null;
        } else if (window == null || window < 1) {
            throw ApiException.badRequest("启用游客额度时必须提供 guestLimitWindowSeconds");
        }
        Optional<Tool> updated = toolRepository.updatePolicy(toolId, request, guestLimit, window);
        if (updated.isEmpty()) {
            throw ApiException.conflict("RESOURCE_VERSION_CONFLICT", "工具策略版本已变化，请重新读取后再修改");
        }
        audit(actor, "TOOL_POLICY_UPDATED", toolId,
                Map.of("expectedVersion", request.expectedVersion(), "previousVersion", old.version(),
                        "guestLimit", guestLimit == null ? "unlimited" : guestLimit,
                        "guestLimitWindowSeconds", window == null ? "unlimited" : window));
        return updated.get();
    }

    @Transactional
    public ToolExecuteData execute(String toolId, ToolExecuteRequest request, AuthenticatedPrincipal principal) {
        Tool tool = publicDetail(toolId);
        if (tool.policy().loginRequired() && principal.isGuest()) {
            throw ApiException.unauthenticated("该工具需要登录后使用");
        }
        ToolExecutor executor = executorRegistry.find(toolId);
        if (executor == null) {
            throw ApiException.toolNotImplemented(toolId);
        }
        String requestHash = requestHash(request);
        String principalKey = principal.principalType().name() + ":" + principal.subject();
        Optional<IdempotencyRecord> existing = idempotencyRepository.find(principalKey, toolId, request.clientRequestId());
        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            if (!record.requestHash().equals(requestHash)) {
                throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "clientRequestId 已用于其他请求");
            }
            if (!record.completed()) {
                throw ApiException.conflict("IDEMPOTENCY_IN_PROGRESS", "相同请求正在执行");
            }
            return deserializeResult(record.responseJson());
        }
        if (!idempotencyRepository.insertPending(principalKey, toolId, request.clientRequestId(), requestHash,
                Instant.now().plus(24, ChronoUnit.HOURS))) {
            IdempotencyRecord raced = idempotencyRepository.find(principalKey, toolId, request.clientRequestId())
                    .orElseThrow(() -> ApiException.unavailable("幂等记录暂不可用"));
            if (!raced.requestHash().equals(requestHash)) {
                throw ApiException.conflict("IDEMPOTENCY_CONFLICT", "clientRequestId 已用于其他请求");
            }
            throw ApiException.conflict("IDEMPOTENCY_IN_PROGRESS", "相同请求正在执行");
        }
        try {
            Usage usage = quotaService.consume(tool, principal);
            Map<String, Object> result = executor.execute(new ToolExecutor.ToolExecutionContext(tool,
                    request.toolVersion() == null ? tool.currentVersion() : request.toolVersion(), request.input(), principal,
                    RequestContext.requestId()));
            String version = request.toolVersion() == null ? tool.currentVersion() : request.toolVersion();
            ToolExecuteData data = new ToolExecuteData(toolId, version, result == null ? Map.of() : result, usage);
            toolRepository.recordUsage(toolId, version, principal.principalType().name(), principal.subject(),
                    request.clientRequestId(), requestHash, "SUCCESS");
            idempotencyRepository.complete(principalKey, toolId, request.clientRequestId(), serialize(data));
            return data;
        } catch (ApiException exception) {
            idempotencyRepository.delete(principalKey, toolId, request.clientRequestId());
            throw exception;
        } catch (DataAccessException exception) {
            idempotencyRepository.delete(principalKey, toolId, request.clientRequestId());
            throw ApiException.unavailable("数据服务暂不可用");
        } catch (Exception exception) {
            idempotencyRepository.delete(principalKey, toolId, request.clientRequestId());
            throw ApiException.dependency("工具执行失败");
        }
    }

    public UsageSummary usage(String toolId, Instant from, Instant to) {
        findTool(toolId);
        Instant normalizedFrom = from == null ? Instant.now().minus(24, ChronoUnit.HOURS) : from;
        Instant normalizedTo = to == null ? Instant.now() : to;
        if (!normalizedFrom.isBefore(normalizedTo) || normalizedTo.isAfter(Instant.now().plusSeconds(5))) {
            throw ApiException.badRequest("from/to 时间范围无效");
        }
        ToolRepository.UsageCounts counts = toolRepository.usage(toolId, normalizedFrom, normalizedTo);
        return new UsageSummary(toolId, normalizedFrom, normalizedTo, counts.total(), counts.guests(), counts.users());
    }

    private Tool findTool(String toolId) {
        return toolRepository.findById(toolId).orElseThrow(() -> ApiException.notFound("工具不存在"));
    }

    private String requestHash(ToolExecuteRequest request) {
        try {
            java.util.Map<String, Object> canonical = new java.util.LinkedHashMap<>();
            canonical.put("toolVersion", request.toolVersion());
            canonical.put("input", request.input());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsBytes(canonical)));
        } catch (Exception exception) {
            throw new IllegalStateException("无法计算请求摘要", exception);
        }
    }

    private String serialize(ToolExecuteData data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法保存工具执行结果", exception);
        }
    }

    private ToolExecuteData deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, ToolExecuteData.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("幂等执行结果损坏", exception);
        }
    }

    private void audit(AuthenticatedPrincipal actor, String action, String toolId, Map<String, Object> details) {
        try {
            auditRepository.record(actor.principalType().name(), actor.subject(), action, "TOOL", toolId,
                    objectMapper.writeValueAsString(details), RequestContext.requestId());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法写入审计日志", exception);
        }
    }

    private int normalizePage(int page) {
        if (page < 1) {
            throw ApiException.badRequest("page 必须从 1 开始");
        }
        return page;
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize < 1 || pageSize > 100) {
            throw ApiException.badRequest("pageSize 必须在 1 到 100 之间");
        }
        return pageSize;
    }
}
