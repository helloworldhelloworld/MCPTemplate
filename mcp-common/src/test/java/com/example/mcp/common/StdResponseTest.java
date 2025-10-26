package com.example.mcp.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class StdResponseTest {

  @Test
  void successResponsePopulatesFields() {
    StdResponse<String> response = StdResponse.success("CODE", "All good", "payload");

    assertEquals("success", response.getStatus());
    assertEquals("CODE", response.getCode());
    assertEquals("All good", response.getMessage());
    assertEquals("payload", response.getData());
  }

  @Test
  void errorResponseLeavesDataNull() {
    StdResponse<Void> response = StdResponse.error("ERR", "Failure");

    assertEquals("error", response.getStatus());
    assertEquals("ERR", response.getCode());
    assertEquals("Failure", response.getMessage());
    assertNull(response.getData());
  }
}
