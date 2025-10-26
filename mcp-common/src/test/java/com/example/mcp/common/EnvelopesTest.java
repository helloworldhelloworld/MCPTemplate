package com.example.mcp.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.example.mcp.common.Envelopes.RequestEnvelope;
import com.example.mcp.common.Envelopes.ResponseEnvelope;
import com.example.mcp.common.Envelopes.StreamEventEnvelope;
import com.example.mcp.common.Envelopes.UiCard;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class EnvelopesTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  void requestEnvelopeDefaultsAndMutators() {
    RequestEnvelope envelope = new RequestEnvelope();
    Context context = new Context();
    ObjectNode payload = mapper.createObjectNode().put("value", "one");

    envelope.setTool("tool.name");
    envelope.setContext(context);
    envelope.setPayload(payload);
    envelope.getAttachments().put("file", "ref");

    assertEquals("tool.name", envelope.getTool());
    assertSame(context, envelope.getContext());
    assertSame(payload, envelope.getPayload());
    assertEquals("ref", envelope.getAttachments().get("file"));
  }

  @Test
  void responseEnvelopeHoldsResponseAndUiCard() {
    ResponseEnvelope<String> envelope = new ResponseEnvelope<>();
    Context context = new Context();
    StdResponse<String> response = StdResponse.success("OK", "done", "data");
    UiCard card = new UiCard();
    card.setTitle("Title");
    card.getActions().put("open", "https://example.com");

    envelope.setTool("tool.response");
    envelope.setContext(context);
    envelope.setResponse(response);
    envelope.setUiCard(card);

    assertEquals("tool.response", envelope.getTool());
    assertSame(context, envelope.getContext());
    assertSame(response, envelope.getResponse());
    assertSame(card, envelope.getUiCard());
    assertEquals("https://example.com", envelope.getUiCard().getActions().get("open"));
  }

  @Test
  void streamEventEnvelopeCapturesEventDetails() {
    StreamEventEnvelope<String> envelope = new StreamEventEnvelope<>();
    OffsetDateTime now = OffsetDateTime.now();
    StdResponse<String> response = StdResponse.success("CODE", "msg", "payload");

    envelope.setTool("tool.stream");
    envelope.setEvent("heartbeat");
    envelope.setEmittedAt(now);
    envelope.setResponse(response);

    assertEquals("tool.stream", envelope.getTool());
    assertEquals("heartbeat", envelope.getEvent());
    assertEquals(now, envelope.getEmittedAt());
    assertSame(response, envelope.getResponse());
    assertNotNull(response.getData());
  }
}
