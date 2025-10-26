package com.example.mcp.client.demo;

import com.example.mcp.client.config.McpClientConfig;
import com.example.mcp.client.config.McpClientConfigLoader;
import com.example.mcp.client.runtime.McpClientEnvironment;
import com.example.mcp.client.runtime.McpRouteClient;
import com.example.mcp.common.StdResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Map;

public class ClientDemo {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  public static void main(String[] args) throws Exception {
    try (InputStream input = ClientDemo.class.getResourceAsStream("/mcp-client-config.json")) {
      if (input == null) {
        throw new IllegalStateException("mcp-client-config.json not found on classpath");
      }
      McpClientConfig config = McpClientConfigLoader.load(input);
      try (McpClientEnvironment environment = new McpClientEnvironment(config)) {
        environment.start();
        McpRouteClient routeClient = new McpRouteClient(environment);

        StdResponse<?> response =
            routeClient.invokeBlocking("vehicleState", Map.of("vehicleId", "demo-vehicle"));
        logResponse(response);
      }
    }
  }

  private static void logResponse(StdResponse<?> response) {
    System.out.println("Vehicle state response status: " + response.getStatus());
    Object data = response.getData();
    if (data == null) {
      System.out.println("No vehicle data returned");
      return;
    }
    JsonNode node = MAPPER.valueToTree(data);
    System.out.println("Vehicle: " + node.path("vehicleId").asText("<unknown>"));
    if (node.hasNonNull("batteryPercentage")) {
      System.out.println("Battery: " + node.path("batteryPercentage").asDouble());
    }
    if (node.hasNonNull("updatedAt")) {
      System.out.println("Updated: " + node.path("updatedAt").asText());
    }
  }
}
