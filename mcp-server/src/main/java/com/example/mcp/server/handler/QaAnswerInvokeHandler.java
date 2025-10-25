package com.example.mcp.server.handler;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import org.springframework.stereotype.Component;

@Component
public class QaAnswerInvokeHandler
    implements ToolHandler<QaAnswerInvokeHandler.QaRequest, QaAnswerInvokeHandler.QaResponse> {

  @Override
  public String getToolName() {
    return "mcp.qa.answer.invoke";
  }

  @Override
  public Class<QaRequest> getRequestType() {
    return QaRequest.class;
  }

  @Override
  public StdResponse<QaResponse> handle(Context context, QaRequest request) {
    QaResponse response = new QaResponse();
    response.setAnswer("Stub answer to: " + request.getQuestion());
    response.setConfidence(0.42);
    Context.Usage usage = new Context.Usage();
    usage.setLatencyMs(18L);
    context.setUsage(usage);
    return StdResponse.success("QA_ANSWERED", "Answer generated", response);
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
