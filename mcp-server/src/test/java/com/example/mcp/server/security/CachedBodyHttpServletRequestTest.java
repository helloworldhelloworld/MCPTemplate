package com.example.mcp.server.security;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import javax.servlet.ServletInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class CachedBodyHttpServletRequestTest {

  @Test
  void cachedBodyCanBeReadMultipleTimes() throws IOException {
    MockHttpServletRequest original = new MockHttpServletRequest();
    original.setContent("payload".getBytes());

    CachedBodyHttpServletRequest cached = new CachedBodyHttpServletRequest(original);

    ServletInputStream first = cached.getInputStream();
    byte[] firstBytes = first.readAllBytes();
    byte[] secondBytes = cached.getInputStream().readAllBytes();

    assertArrayEquals("payload".getBytes(), firstBytes);
    assertArrayEquals("payload".getBytes(), secondBytes);
    assertEquals("payload", cached.getCachedBodyAsString());
  }
}
