package com.example.mcp.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

public final class Envelopes {
  private Envelopes() {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class RequestEnvelope implements Serializable {
    @JsonProperty("tool")
    private String tool;

    @JsonProperty("context")
    private Context context;

    @JsonProperty("payload")
    private JsonNode payload;

    @JsonProperty("attachments")
    private Map<String, String> attachments = new HashMap<>();

    public String getTool() {
      return tool;
    }

    public void setTool(String tool) {
      this.tool = tool;
    }

    public Context getContext() {
      return context;
    }

    public void setContext(Context context) {
      this.context = context;
    }

    public JsonNode getPayload() {
      return payload;
    }

    public void setPayload(JsonNode payload) {
      this.payload = payload;
    }

    public Map<String, String> getAttachments() {
      return attachments;
    }

    public void setAttachments(Map<String, String> attachments) {
      this.attachments = attachments;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class ResponseEnvelope<T> implements Serializable {
    @JsonProperty("tool")
    private String tool;

    @JsonProperty("context")
    private Context context;

    @JsonProperty("response")
    private StdResponse<T> response;

    @JsonProperty("uiCard")
    private UiCard uiCard;

    public String getTool() {
      return tool;
    }

    public void setTool(String tool) {
      this.tool = tool;
    }

    public Context getContext() {
      return context;
    }

    public void setContext(Context context) {
      this.context = context;
    }

    public StdResponse<T> getResponse() {
      return response;
    }

    public void setResponse(StdResponse<T> response) {
      this.response = response;
    }

    public UiCard getUiCard() {
      return uiCard;
    }

    public void setUiCard(UiCard uiCard) {
      this.uiCard = uiCard;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class StreamEventEnvelope<T> implements Serializable {
    @JsonProperty("tool")
    private String tool;

    @JsonProperty("event")
    private String event;

    @JsonProperty("emittedAt")
    private OffsetDateTime emittedAt;

    @JsonProperty("response")
    private StdResponse<T> response;

    public String getTool() {
      return tool;
    }

    public void setTool(String tool) {
      this.tool = tool;
    }

    public String getEvent() {
      return event;
    }

    public void setEvent(String event) {
      this.event = event;
    }

    public OffsetDateTime getEmittedAt() {
      return emittedAt;
    }

    public void setEmittedAt(OffsetDateTime emittedAt) {
      this.emittedAt = emittedAt;
    }

    public StdResponse<T> getResponse() {
      return response;
    }

    public void setResponse(StdResponse<T> response) {
      this.response = response;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class UiCard implements Serializable {
    @JsonProperty("title")
    private String title;

    @JsonProperty("subtitle")
    private String subtitle;

    @JsonProperty("body")
    private String body;

    @JsonProperty("actions")
    private Map<String, String> actions = new HashMap<>();

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getSubtitle() {
      return subtitle;
    }

    public void setSubtitle(String subtitle) {
      this.subtitle = subtitle;
    }

    public String getBody() {
      return body;
    }

    public void setBody(String body) {
      this.body = body;
    }

    public Map<String, String> getActions() {
      return actions;
    }

    public void setActions(Map<String, String> actions) {
      this.actions = actions;
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public static class GlossaryEntry implements Serializable {
    @JsonProperty("term")
    private String term;

    @JsonProperty("definition")
    private String definition;

    public String getTerm() {
      return term;
    }

    public void setTerm(String term) {
      this.term = term;
    }

    public String getDefinition() {
      return definition;
    }

    public void setDefinition(String definition) {
      this.definition = definition;
    }
  }
}
