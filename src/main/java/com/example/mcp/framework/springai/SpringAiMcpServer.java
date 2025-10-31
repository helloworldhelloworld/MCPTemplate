package com.example.mcp.framework.springai;

import com.example.mcp.common.Context;
import com.example.mcp.common.protocol.SessionOpenRequest;
import com.example.mcp.framework.server.AbstractMcpServer;

public class SpringAiMcpServer extends AbstractMcpServer {

    private final String modelName;

    public SpringAiMcpServer(String modelName) {
        this.modelName = modelName;
    }

    @Override
    protected Context buildSessionContext(SessionOpenRequest request) {
        Context context = super.buildSessionContext(request);
        context.putMetadata("springai-model", modelName);
        return context;
    }
}
