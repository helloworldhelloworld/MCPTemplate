package com.example.mcp.server.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.mcp.common.StdResponse;
import org.junit.jupiter.api.Test;

class QaAnswerMcpToolTest {

    @Test
    void returnsAnswerWithConfidence() {
        QaAnswerMcpTool tool = new QaAnswerMcpTool();
        QaAnswerMcpTool.QaAnswerRequest request = new QaAnswerMcpTool.QaAnswerRequest();
        request.setQuestion("春季新品什么时候发布？");

        StdResponse<QaAnswerMcpTool.QaAnswerResponse> response = tool.answer(request);

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData().getAnswer()).contains("春季新品");
        assertThat(response.getData().getConfidence()).isBetween(0.0, 1.0);
    }
}
