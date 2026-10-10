package com.xujiache.jingweitoolbox.tool;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ToolExecutorRegistry {
    private final Map<String, ToolExecutor> executors;

    public ToolExecutorRegistry(List<ToolExecutor> executors) {
        this.executors = executors.stream().collect(Collectors.toUnmodifiableMap(ToolExecutor::toolId, Function.identity()));
    }

    public ToolExecutor find(String toolId) {
        return executors.get(toolId);
    }
}
