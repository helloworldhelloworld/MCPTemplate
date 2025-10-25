package com.example.mcp.client.api;

import com.example.mcp.client.McpClient;
import com.example.mcp.common.StdResponse;

public class QaApi {
  private final McpClient client;

  public QaApi(McpClient client) {
    this.client = client;
  }

  public StdResponse<QaResponse> ask(String question, String context) throws Exception {
    QaRequest request = new QaRequest();
    request.setQuestion(question);
    request.setContext(context);
    return client.invoke("mcp.qa.answer.invoke", request, QaResponse.class);
  }

  public static class QaRequest {
    private String question;
    private String context;

    public String getQuestion() {
      return question;
    }

    public void setQuestion(String question) {
      this.question = question;
    }

    public String getContext() {
      return context;
    }

    public void setContext(String context) {
      this.context = context;
    }
  }

  public static class QaResponse {
    private String answer;
    private Double confidence;

    public String getAnswer() {
      return answer;
    }

    public void setAnswer(String answer) {
      this.answer = answer;
    }

    public Double getConfidence() {
      return confidence;
    }

    public void setConfidence(Double confidence) {
      this.confidence = confidence;
    }
  }
}
