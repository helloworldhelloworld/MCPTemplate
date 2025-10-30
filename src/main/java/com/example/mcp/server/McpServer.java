package com.example.mcp.server;

import com.example.mcp.common.Context;
import com.example.mcp.common.Envelopes;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.protocol.GovernanceReport;
import com.example.mcp.common.protocol.InvocationAuditRecord;
import com.example.mcp.common.protocol.SessionOpenRequest;
import com.example.mcp.common.protocol.SessionOpenResponse;
import com.example.mcp.common.protocol.ToolDescriptor;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory MCP server that registers a collection of tool handlers and exposes the
 * interaction primitives used by {@link com.example.mcp.client.McpClient}.
 */
public final class McpServer {

    private final Map<String, ToolRegistration<?, ?>> registrations = new HashMap<>();
    private final GovernanceReport governanceReport = new GovernanceReport();

    public <I, O> void registerTool(ToolDescriptor descriptor, Class<I> inputType,
                                    ToolHandler<I, O> handler, Class<O> outputType) {
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(inputType, "inputType");
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(outputType, "outputType");
        registrations.put(descriptor.getName(), new ToolRegistration<>(descriptor, inputType, handler, outputType));
    }

    public SessionOpenResponse openSession(SessionOpenRequest request) {
        Context context = new Context();
        context.setClientId(Optional.ofNullable(request.getClientId()).orElse("anonymous"));
        context.setSessionId(UUID.randomUUID().toString());
        context.setLocale(request.getLocale());
        request.getMetadata().forEach(context::putMetadata);
        context.putMetadata("session-opened-at", Instant.now().toString());
        String greeting = "欢迎使用 MCP 工具服务, " + context.getClientId() + '!';
        return new SessionOpenResponse(context.getSessionId(), context, greeting);
    }

    public List<ToolDescriptor> listTools() {
        return new ArrayList<>(registrations.values().stream().map(ToolRegistration::descriptor).toList());
    }

    public Optional<ToolDescriptor> describeTool(String name) {
        ToolRegistration<?, ?> registration = registrations.get(name);
        return Optional.ofNullable(registration == null ? null : registration.descriptor());
    }

    @SuppressWarnings("unchecked")
    public <I, O> Envelopes.ResponseEnvelope<O> invoke(Envelopes.RequestEnvelope<I> request, Class<O> responseType) {
        ToolRegistration<I, O> registration = (ToolRegistration<I, O>) registrations.get(request.getTool());
        if (registration == null) {
            StdResponse<O> response = StdResponse.error("tool_not_found", "Unknown tool: " + request.getTool());
            return new Envelopes.ResponseEnvelope<>(request.getTool(), request.getContext(), response, null);
        }
        if (!registration.outputType().equals(responseType)) {
            StdResponse<O> response = StdResponse.error("type_mismatch", "Unexpected response type for tool " + request.getTool());
            return new Envelopes.ResponseEnvelope<>(request.getTool(), request.getContext(), response, null);
        }

        Context invocationContext = request.getContext().copy();
        invocationContext.setRequestId(UUID.randomUUID().toString());
        long start = System.currentTimeMillis();
        StdResponse<O> response = registration.handler().handle(invocationContext, request.getPayload());
        long latency = System.currentTimeMillis() - start;
        invocationContext.getUsage().setLatencyMs(latency);
        invocationContext.getUsage().setInputTokens(invocationContext.getUsage().getInputTokens() + request.getPayload().toString().length());
        O data = response.getData();
        if (data != null) {
            invocationContext.getUsage().setOutputTokens(invocationContext.getUsage().getOutputTokens() + data.toString().length());
        }
        governanceReport.addRecord(new InvocationAuditRecord(invocationContext.getRequestId(), request.getTool(),
                response.getStatus(), latency, Instant.now()));
        Envelopes.UiCard card = buildUiCard(request.getTool(), response);
        return new Envelopes.ResponseEnvelope<>(request.getTool(), invocationContext, response, card);
    }

    public GovernanceReport governanceReport() {
        return governanceReport;
    }

    private Envelopes.UiCard buildUiCard(String tool, StdResponse<?> response) {
        Envelopes.UiCard card = new Envelopes.UiCard();
        card.setTitle("工具 " + tool + " 执行结果");
        card.setBody(response.getMessage());
        card.getActions().put("trace", "查看治理日志");
        return card;
    }

    private record ToolRegistration<I, O>(ToolDescriptor descriptor, Class<I> inputType,
                                          ToolHandler<I, O> handler, Class<O> outputType) {
    }
}
