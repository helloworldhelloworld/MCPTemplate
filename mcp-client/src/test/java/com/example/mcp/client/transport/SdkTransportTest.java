package com.example.mcp.client.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class SdkTransportTest {

  @Test
  void constructorRequiresExpectedMethods() {
    assertThrows(IllegalArgumentException.class, () -> new SdkTransport(new Object()));
  }

  @Test
  void delegatesToSdkClient() throws Exception {
    RecordingSdk sdk = new RecordingSdk();
    SdkTransport transport = new SdkTransport(sdk);

    String response = transport.postJson("/mcp/invoke", "{\"a\":1}");
    List<String> events = new ArrayList<>();
    transport.getSse("/mcp/stream", events::add);

    assertEquals("/mcp/invoke", sdk.lastPath);
    assertEquals("{\"a\":1}", sdk.lastPayload);
    assertEquals("response", response);
    assertEquals(1, events.size());
    assertEquals("event", events.get(0));
  }

  @Test
  void autoDetectThrowsWhenSdkMissing() {
    assertThrows(IllegalStateException.class, SdkTransport::autoDetect);
  }

  private static class RecordingSdk {
    private String lastPath;
    private String lastPayload;

    public String postJson(String path, String json) {
      lastPath = path;
      lastPayload = json;
      return "response";
    }

    public void getSse(String path, Consumer<String> consumer) {
      consumer.accept("event");
    }
  }
}
