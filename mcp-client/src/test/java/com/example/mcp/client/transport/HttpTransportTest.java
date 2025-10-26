package com.example.mcp.client.transport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpTransportTest {

  private MockWebServer server;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  void postJsonReturnsResponseBody() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(200).setBody("{\"ok\":true}"));
    HttpTransport transport = new HttpTransport(server.url("/").toString());

    String response = transport.postJson("/mcp/invoke", "{\"hello\":\"world\"}");

    assertEquals("{\"ok\":true}", response);
    assertEquals("POST", server.takeRequest(1, TimeUnit.SECONDS).getMethod());
  }

  @Test
  void postJsonThrowsOnErrorStatus() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(500));
    HttpTransport transport = new HttpTransport(server.url("/").toString());

    assertThrows(
        IllegalStateException.class, () -> transport.postJson("/mcp/invoke", "{\"hello\":\"world\"}"));
  }

  @Test
  void getSseCollectsNonEmptyLines() throws Exception {
    server.enqueue(
        new MockResponse()
            .setResponseCode(200)
            .setBody("data: one\n\n\ndata: two\n\n")
            .addHeader("Content-Type", "text/event-stream"));
    HttpTransport transport = new HttpTransport(server.url("/").toString());
    List<String> events = new ArrayList<>();

    transport.getSse("/mcp/stream", events::add);

    assertEquals(2, events.size());
    assertEquals("data: one", events.get(0));
    assertEquals("data: two", events.get(1));
  }
}
