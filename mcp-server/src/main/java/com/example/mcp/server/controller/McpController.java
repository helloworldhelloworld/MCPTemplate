package com.example.mcp.server.controller;

import com.example.mcp.common.Context;
import com.example.mcp.common.Envelopes.RequestEnvelope;
import com.example.mcp.common.Envelopes.ResponseEnvelope;
import com.example.mcp.common.Envelopes.StreamEventEnvelope;
import com.example.mcp.common.Envelopes.UiCard;
import com.example.mcp.common.StdResponse;
import com.example.mcp.server.handler.ToolHandler;
import com.example.mcp.server.handler.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class McpController {
  private static final Logger log = LoggerFactory.getLogger(McpController.class);
  private final ToolRegistry toolRegistry;
  private final ObjectMapper objectMapper;

  public McpController(ToolRegistry toolRegistry, ObjectMapper objectMapper) {
    this.toolRegistry = toolRegistry;
    this.objectMapper = objectMapper;
  }

  @PostMapping(path = "/mcp/invoke", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<?> invoke(
      @RequestBody RequestEnvelope requestEnvelope, HttpServletRequest httpRequest) {
    try {
      if (requestEnvelope.getContext() == null) {
        requestEnvelope.setContext(new Context());
      }
      Context context = requestEnvelope.getContext();
      if (context.getRequestId() == null) {
        context.setRequestId(UUID.randomUUID().toString());
      }
      if (context.getTimestamp() == null) {
        context.setTimestamp(OffsetDateTime.now());
      }
      if (context.getClientId() == null) {
        context.setClientId(httpRequest.getHeader("X-MCP-ClientId"));
      }
      httpRequest.setAttribute("mcp.tool.name", requestEnvelope.getTool());

      ToolHandler<?, ?> handler =
          toolRegistry
              .find(requestEnvelope.getTool())
              .orElseThrow(() -> new IllegalArgumentException("Unknown tool"));
      Object requestBody = convertPayload(requestEnvelope.getPayload(), handler.getRequestType());
      @SuppressWarnings("unchecked")
      ToolHandler<Object, Object> typedHandler = (ToolHandler<Object, Object>) handler;
      StdResponse<Object> response = typedHandler.handle(context, requestBody);

      ResponseEnvelope<Object> envelope = new ResponseEnvelope<>();
      envelope.setTool(requestEnvelope.getTool());
      envelope.setContext(context);
      envelope.setResponse(response);
      if ("mcp.translation.invoke".equals(requestEnvelope.getTool())) {
        UiCard card = buildTranslationCard(response, context);
        if (card != null) {
          envelope.setUiCard(card);
        }
      }
      return ResponseEntity.ok(envelope);
    } catch (IllegalArgumentException ex) {
      log.warn("Invocation error: {}", ex.getMessage());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST)
          .body(StdResponse.error("INVALID_REQUEST", ex.getMessage()));
    } catch (Exception ex) {
      log.error("Failed to process tool invocation", ex);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body(StdResponse.error("INTERNAL_ERROR", "Failed to invoke tool"));
    }
  }

  @GetMapping(path = "/mcp/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamEvents() {
    SseEmitter emitter = new SseEmitter();
    new Thread(
            () -> {
              try {
                StreamEventEnvelope<String> eventEnvelope = new StreamEventEnvelope<>();
                eventEnvelope.setTool("mcp.translation.invoke");
                eventEnvelope.setEvent("heartbeat");
                eventEnvelope.setEmittedAt(OffsetDateTime.now());
                eventEnvelope.setResponse(
                    StdResponse.success("STREAM", "Server heartbeat", "alive"));
                emitter.send(eventEnvelope);
                emitter.complete();
              } catch (Exception ex) {
                emitter.completeWithError(ex);
              }
            })
        .start();
    return emitter;
  }

  private <T> T convertPayload(JsonNode payload, Class<T> type) {
    if (payload == null || payload.isNull()) {
      return null;
    }
    return objectMapper.convertValue(payload, type);
  }

  private UiCard buildTranslationCard(StdResponse<Object> response, Context context) {
    if (response == null) {
      return null;
    }
    Map<String, String> metadata = context != null ? context.getMetadata() : null;
    if (metadata == null) {
      metadata = new HashMap<>();
      if (context != null) {
        context.setMetadata(metadata);
      }
    }

    String status = response.getStatus();
    UiCard card = new UiCard();
    if ("clarify".equalsIgnoreCase(status)) {
      card.setTitle("澄清请求");
      String prompt = metadata.getOrDefault("clarify.prompt", response.getMessage());
      card.setBody(prompt);
      String missingField = metadata.get("clarify.missingField");
      if ("targetLanguage".equals(missingField)) {
        card.getActions().put("选择中文", "targetLanguage=zh-CN");
        card.getActions().put("选择英文", "targetLanguage=en-US");
        card.getActions().put("选择日文", "targetLanguage=ja-JP");
      } else if ("text".equals(missingField)) {
        card.getActions().put("输入原文", "text=<请输入需要翻译的内容>");
        String examples = metadata.get("clarify.examples");
        if (examples != null) {
          card.setSubtitle(examples);
        }
      }
      return card;
    }

    if (!"success".equalsIgnoreCase(status)) {
      return null;
    }

    card.setTitle("Translation Result");
    Object data = response.getData();
    if (data != null) {
      String translated =
          objectMapper.convertValue(data, JsonNode.class).path("translatedText").asText("");
      card.setBody(translated);
    }
    return card;
  }
}
