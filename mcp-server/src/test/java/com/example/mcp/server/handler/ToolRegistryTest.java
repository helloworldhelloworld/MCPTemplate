package com.example.mcp.server.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.mcp.common.Context;
import com.example.mcp.common.StdResponse;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {

  @Test
  void registryFindsHandlersByName() throws Exception {
    ToolHandler<String, String> first =
        new ToolHandler<String, String>() {
          @Override
          public String getToolName() {
            return "tool.one";
          }

          @Override
          public Class<String> getRequestType() {
            return String.class;
          }

          @Override
          public StdResponse<String> handle(Context context, String request) {
            return StdResponse.success("OK", "handled", request);
          }
        };
    ToolHandler<String, String> second =
        new ToolHandler<String, String>() {
          @Override
          public String getToolName() {
            return "tool.two";
          }

          @Override
          public Class<String> getRequestType() {
            return String.class;
          }

          @Override
          public StdResponse<String> handle(Context context, String request) {
            return StdResponse.success("OK", "handled", request);
          }
        };

    ToolRegistry registry = new ToolRegistry(Arrays.asList(first, second));

    Optional<ToolHandler<?, ?>> found = registry.find("tool.two");
    assertTrue(found.isPresent());
    assertEquals("tool.two", found.get().getToolName());
    assertEquals(2, registry.getHandlers().size());
  }
}
