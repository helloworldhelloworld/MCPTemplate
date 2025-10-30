package com.example.mcp.server.tool;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import com.example.mcp.common.qa.QaRequest;
import com.example.mcp.common.qa.QaResponse;
import com.example.mcp.server.ToolHandler;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class QaTool implements ToolHandler<QaRequest, QaResponse> {

    private final Map<String, String> knowledgeBase = new LinkedHashMap<>();

    public QaTool() {
        knowledgeBase.put("what is mcp", "MCP 是模型与工具之间的标准协议，简化能力编排。");
        knowledgeBase.put("how to cancel a call", "客户端向服务端发送 cancel_call 事件即可中止正在执行的工具。");
        knowledgeBase.put("vehicle safety", "车控指令会在治理层验证司机权限与地理围栏。");
    }

    @Override
    public StdResponse<QaResponse> handle(Context context, QaRequest input) {
        String key = input.getQuestion().toLowerCase(Locale.ROOT).trim();
        String answer = knowledgeBase.get(key);
        if (answer == null) {
            return StdResponse.clarification("qa_follow_up", "没有找到精确答案，需要补充上下文。", new QaResponse("Could you rephrase the question?", 0.2));
        }
        return StdResponse.success("qa_answer", "找到匹配的知识条目", new QaResponse(answer, 0.85));
    }
}
