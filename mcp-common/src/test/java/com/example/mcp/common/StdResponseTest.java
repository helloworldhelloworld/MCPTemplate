package com.example.mcp.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class StdResponseTest {

  @Test
  void clarifyResponseUsesClarifyStatus() {
    StdResponse<String> response = StdResponse.clarify("NEED_INFO", "请补充信息", null);

    assertEquals("clarify", response.getStatus());
    assertEquals("NEED_INFO", response.getCode());
    assertEquals("请补充信息", response.getMessage());
    assertNull(response.getData());
  }
}
