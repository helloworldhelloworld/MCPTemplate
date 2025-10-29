package com.example.mcp.server.tool;

import com.example.mcp.common.StdResponse;
import org.springframework.ai.mcp.server.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通过 Spring AI MCP 暴露一个简单的问答工具。
 */
@Component
public class QaAnswerMcpTool {

    @McpTool(
            name = "mcp.qa.answer.invoke",
            description = "根据 FAQ 数据库返回最佳答案",
            inputSchema = "schema/qa-answer-request.json",
            outputSchema = "schema/qa-answer-response.json")
    public StdResponse<QaAnswerResponse> answer(QaAnswerRequest request) {
        QaAnswerResponse response = new QaAnswerResponse();
        response.setQuestion(request.getQuestion());
        response.setAnswer("这是针对 \"" + request.getQuestion() + "\" 的示例回答");
        response.setConfidence(0.82);
        return StdResponse.success("QA_RESOLVED", "回答已生成", response);
    }

    public static class QaAnswerRequest {
        private String question;
        private Map<String, Object> context = new LinkedHashMap<>();

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public Map<String, Object> getContext() {
            return context;
        }

        public void setContext(Map<String, Object> context) {
            this.context = context;
        }
    }

    public static class QaAnswerResponse {
        private String question;
        private String answer;
        private double confidence;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }
    }
}
