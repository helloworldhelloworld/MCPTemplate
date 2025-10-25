package com.example.mcp.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Context implements Serializable {
  @JsonProperty("clientId")
  private String clientId;

  @JsonProperty("requestId")
  private String requestId;

  @JsonProperty("traceId")
  private String traceId;

  @JsonProperty("timestamp")
  private OffsetDateTime timestamp;

  @JsonProperty("locale")
  private String locale;

  @JsonProperty("metadata")
  private Map<String, String> metadata = new HashMap<>();

  @JsonProperty("usage")
  private Usage usage;

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public OffsetDateTime getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(OffsetDateTime timestamp) {
    this.timestamp = timestamp;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public Usage getUsage() {
    return usage;
  }

  public void setUsage(Usage usage) {
    this.usage = usage;
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class Usage implements Serializable {
    @JsonProperty("inputTokens")
    private Integer inputTokens;

    @JsonProperty("outputTokens")
    private Integer outputTokens;

    @JsonProperty("latencyMs")
    private Long latencyMs;

    public Integer getInputTokens() {
      return inputTokens;
    }

    public void setInputTokens(Integer inputTokens) {
      this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens() {
      return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens) {
      this.outputTokens = outputTokens;
    }

    public Long getLatencyMs() {
      return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
      this.latencyMs = latencyMs;
    }
  }
}
