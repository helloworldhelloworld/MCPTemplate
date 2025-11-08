package com.example.mcp.framework.server;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;

import java.util.HashSet;
import java.util.Set;

/**
 * 安全拦截器，用于访问控制和权限验证
 */
public class SecurityInterceptor implements McpServerInterceptor {

    private final Set<String> allowedClients = new HashSet<>();
    private final Set<String> blockedClients = new HashSet<>();
    private boolean enableWhitelist = false;

    public SecurityInterceptor() {
    }

    public void addAllowedClient(String clientId) {
        allowedClients.add(clientId);
        enableWhitelist = true;
    }

    @Override
    public <I> boolean beforeHandle(String toolName, Context context, I payload) {
        String clientId = context.getClientId();

        // 检查黑名单
        if (blockedClients.contains(clientId)) {
            System.err.println("[MCP-SERVER] 拒绝访问: 客户端 " + clientId + " 在黑名单中");
            return false;
        }

        // 检查白名单
        if (enableWhitelist && !allowedClients.contains(clientId)) {
            System.err.println("[MCP-SERVER] 拒绝访问: 客户端 " + clientId + " 不在白名单中");
            return false;
        }

        return true;
    }

    @Override
    public <I, O> void afterHandle(String toolName, Context context, I payload, StdResponse<O> response) {
    }

    @Override
    public <I> void onError(String toolName, Context context, I payload, Exception exception) {
    }
}
