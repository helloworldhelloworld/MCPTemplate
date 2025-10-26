package com.example.mcp.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextTest {

  @Test
  void settersAndGettersRoundTrip() {
    Context context = new Context();
    OffsetDateTime now = OffsetDateTime.now();
    Map<String, String> metadata = new HashMap<>();
    metadata.put("key", "value");

    context.setClientId("client");
    context.setRequestId("request");
    context.setTraceId("trace");
    context.setTimestamp(now);
    context.setLocale("en-US");
    context.setMetadata(metadata);

    Context.Usage usage = new Context.Usage();
    usage.setInputTokens(10);
    usage.setOutputTokens(20);
    usage.setLatencyMs(30L);
    context.setUsage(usage);

    assertEquals("client", context.getClientId());
    assertEquals("request", context.getRequestId());
    assertEquals("trace", context.getTraceId());
    assertEquals(now, context.getTimestamp());
    assertEquals("en-US", context.getLocale());
    assertSame(metadata, context.getMetadata());
    assertSame(usage, context.getUsage());
    assertEquals(10, context.getUsage().getInputTokens());
    assertEquals(20, context.getUsage().getOutputTokens());
    assertEquals(30L, context.getUsage().getLatencyMs());
  }
}
